package net.beamex.shamaschizm.entity.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import net.beamex.shamaschizm.Shamaschizm;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Minimal glTF 2.0 mesh/animation reader for the supplied Blockbench body.
 * It keeps the exported per-face UV coordinates exactly; converting these
 * meshes back into Minecraft box UVs would visibly scramble the texture.
 */
public final class GiantCentipedeBodyMesh {
    private static final float CENTER_X = 0.0F;
    private static final float MIN_Y = -0.01704952F;
    private static final float CENTER_Z = -0.03125F;

    private final byte[] binary;
    private final BufferView[] views;
    private final Accessor[] accessors;
    private final Mesh[] meshes;
    private final Node[] nodes;
    private final int[] sceneRoots;
    private final List<Channel> channels;
    private final float duration;

    public GiantCentipedeBodyMesh(ResourceManager resources) {
        try (var reader = new InputStreamReader(resources.open(
                Shamaschizm.id("models/entity/giant_centipede_body.gltf")))) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            String bufferUri = root.getAsJsonArray("buffers").get(0).getAsJsonObject().get("uri").getAsString();
            this.binary = Base64.getDecoder().decode(bufferUri.substring(bufferUri.indexOf(',') + 1));
            this.views = readViews(root.getAsJsonArray("bufferViews"));
            this.accessors = readAccessors(root.getAsJsonArray("accessors"));
            this.meshes = readMeshes(root.getAsJsonArray("meshes"));
            this.nodes = readNodes(root.getAsJsonArray("nodes"));
            JsonObject scene = root.getAsJsonArray("scenes").get(root.has("scene") ? root.get("scene").getAsInt() : 0).getAsJsonObject();
            this.sceneRoots = ints(scene.getAsJsonArray("nodes"));
            Animation animation = readAnimation(root.getAsJsonArray("animations").get(0).getAsJsonObject());
            this.channels = animation.channels;
            this.duration = animation.duration;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load giant centipede body glTF", exception);
        }
    }

    public void render(PoseStack.Pose pose, VertexConsumer consumer, int light, int overlay,
                       float ageInTicks, boolean moving) {
        Vector3f[] translations = new Vector3f[this.nodes.length];
        Quaternionf[] rotations = new Quaternionf[this.nodes.length];
        Vector3f[] scales = new Vector3f[this.nodes.length];
        for (int i = 0; i < this.nodes.length; i++) {
            translations[i] = new Vector3f(this.nodes[i].translation);
            rotations[i] = new Quaternionf(this.nodes[i].rotation);
            scales[i] = new Vector3f(this.nodes[i].scale);
        }
        // The supplied walk cycle was authored in the wrong direction.
        float forwardTime = moving ? (ageInTicks / 20.0F) % this.duration : 0.0F;
        float reverseTime = moving && forwardTime > 0.0F ? this.duration - forwardTime : 0.0F;
        for (Channel channel : this.channels) channel.apply(reverseTime, translations, rotations, scales);
        Matrix4f identity = new Matrix4f();
        for (int root : this.sceneRoots) renderNode(root, identity, translations, rotations, scales,
                pose, consumer, light, overlay);
    }

    private void renderNode(int index, Matrix4f parent, Vector3f[] translations, Quaternionf[] rotations,
                            Vector3f[] scales, PoseStack.Pose pose, VertexConsumer consumer,
                            int light, int overlay) {
        Node node = this.nodes[index];
        Matrix4f local = new Matrix4f().translation(translations[index])
                .rotate(rotations[index]).scale(scales[index]);
        Matrix4f world = new Matrix4f(parent).mul(local);
        if (node.mesh >= 0) renderMesh(this.meshes[node.mesh], world, pose, consumer, light, overlay);
        for (int child : node.children) renderNode(child, world, translations, rotations, scales,
                pose, consumer, light, overlay);
    }

    private void renderMesh(Mesh mesh, Matrix4f world, PoseStack.Pose pose, VertexConsumer consumer,
                            int light, int overlay) {
        float[] positions = floats(mesh.positionAccessor);
        float[] normals = floats(mesh.normalAccessor);
        float[] uvs = floats(mesh.uvAccessor);
        int[] indices = indices(mesh.indexAccessor);
        for (int triangle = 0; triangle + 2 < indices.length; triangle += 3) {
            int a = indices[triangle];
            int b = indices[triangle + 1];
            int c = indices[triangle + 2];
            vertex(a, positions, normals, uvs, world, pose, consumer, light, overlay);
            vertex(b, positions, normals, uvs, world, pose, consumer, light, overlay);
            vertex(c, positions, normals, uvs, world, pose, consumer, light, overlay);
            // Entity render pipelines consume quads. Repeating the last point
            // turns each glTF triangle into an equivalent degenerate quad.
            vertex(c, positions, normals, uvs, world, pose, consumer, light, overlay);
        }
    }

    private static void vertex(int index, float[] positions, float[] normals, float[] uvs,
                               Matrix4f world, PoseStack.Pose pose, VertexConsumer consumer,
                               int light, int overlay) {
        Vector3f source = new Vector3f(positions[index * 3], positions[index * 3 + 1], positions[index * 3 + 2]);
        world.transformPosition(source);
        // Keep Blockbench's original axes. X is the section's width (including
        // its legs) and Z is its 0.625-block longitudinal shell dimension.
        Vector3f model = new Vector3f(source.x - CENTER_X, source.y - MIN_Y, source.z - CENTER_Z);
        pose.pose().transformPosition(model);

        Vector3f normal = new Vector3f(normals[index * 3], normals[index * 3 + 1], normals[index * 3 + 2]);
        world.transformDirection(normal);
        normal.normalize();
        pose.transformNormal(normal, normal);
        consumer.addVertex(model.x, model.y, model.z, -1, uvs[index * 2], uvs[index * 2 + 1],
                overlay, light, normal.x, normal.y, normal.z);
    }

    private float[] floats(int accessorIndex) {
        Accessor accessor = this.accessors[accessorIndex];
        BufferView view = this.views[accessor.view];
        int components = components(accessor.type);
        int stride = view.stride > 0 ? view.stride : components * 4;
        ByteBuffer buffer = ByteBuffer.wrap(this.binary).order(ByteOrder.LITTLE_ENDIAN);
        float[] result = new float[accessor.count * components];
        int start = view.offset + accessor.offset;
        for (int i = 0; i < accessor.count; i++) {
            for (int component = 0; component < components; component++) {
                result[i * components + component] = buffer.getFloat(start + i * stride + component * 4);
            }
        }
        return result;
    }

    private int[] indices(int accessorIndex) {
        Accessor accessor = this.accessors[accessorIndex];
        BufferView view = this.views[accessor.view];
        ByteBuffer buffer = ByteBuffer.wrap(this.binary).order(ByteOrder.LITTLE_ENDIAN);
        int size = accessor.componentType == 5125 ? 4 : accessor.componentType == 5123 ? 2 : 1;
        int stride = view.stride > 0 ? view.stride : size;
        int[] result = new int[accessor.count];
        int start = view.offset + accessor.offset;
        for (int i = 0; i < accessor.count; i++) {
            int at = start + i * stride;
            result[i] = accessor.componentType == 5125 ? buffer.getInt(at)
                    : accessor.componentType == 5123 ? Short.toUnsignedInt(buffer.getShort(at))
                    : Byte.toUnsignedInt(buffer.get(at));
        }
        return result;
    }

    private BufferView[] readViews(JsonArray array) {
        BufferView[] result = new BufferView[array.size()];
        for (int i = 0; i < result.length; i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            result[i] = new BufferView(integer(object, "byteOffset", 0),
                    integer(object, "byteStride", 0));
        }
        return result;
    }

    private Accessor[] readAccessors(JsonArray array) {
        Accessor[] result = new Accessor[array.size()];
        for (int i = 0; i < result.length; i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            result[i] = new Accessor(object.get("bufferView").getAsInt(), integer(object, "byteOffset", 0),
                    object.get("count").getAsInt(), object.get("componentType").getAsInt(),
                    object.get("type").getAsString());
        }
        return result;
    }

    private Mesh[] readMeshes(JsonArray array) {
        Mesh[] result = new Mesh[array.size()];
        for (int i = 0; i < result.length; i++) {
            JsonObject primitive = array.get(i).getAsJsonObject().getAsJsonArray("primitives").get(0).getAsJsonObject();
            JsonObject attributes = primitive.getAsJsonObject("attributes");
            result[i] = new Mesh(attributes.get("POSITION").getAsInt(), attributes.get("NORMAL").getAsInt(),
                    attributes.get("TEXCOORD_0").getAsInt(), primitive.get("indices").getAsInt());
        }
        return result;
    }

    private Node[] readNodes(JsonArray array) {
        Node[] result = new Node[array.size()];
        for (int i = 0; i < result.length; i++) {
            JsonObject object = array.get(i).getAsJsonObject();
            boolean collisionGuide = object.has("name") && object.get("name").getAsString().equals("hitboks");
            result[i] = new Node(integer(object, "mesh", -1),
                    collisionGuide ? new int[0]
                            : object.has("children") ? ints(object.getAsJsonArray("children")) : new int[0],
                    vector(object.getAsJsonArray("translation"), 0.0F, 0.0F, 0.0F),
                    quaternion(object.getAsJsonArray("rotation")),
                    vector(object.getAsJsonArray("scale"), 1.0F, 1.0F, 1.0F));
        }
        return result;
    }

    private Animation readAnimation(JsonObject animation) {
        JsonArray samplersJson = animation.getAsJsonArray("samplers");
        Sampler[] samplers = new Sampler[samplersJson.size()];
        float duration = 0.0F;
        for (int i = 0; i < samplers.length; i++) {
            JsonObject object = samplersJson.get(i).getAsJsonObject();
            float[] times = floats(object.get("input").getAsInt());
            float[] values = floats(object.get("output").getAsInt());
            samplers[i] = new Sampler(times, values);
            if (times.length > 0) duration = Math.max(duration, times[times.length - 1]);
        }
        List<Channel> channels = new ArrayList<>();
        for (JsonElement element : animation.getAsJsonArray("channels")) {
            JsonObject object = element.getAsJsonObject();
            JsonObject target = object.getAsJsonObject("target");
            channels.add(new Channel(target.get("node").getAsInt(), target.get("path").getAsString(),
                    samplers[object.get("sampler").getAsInt()]));
        }
        return new Animation(channels, duration);
    }

    private static int integer(JsonObject object, String name, int fallback) {
        return object.has(name) ? object.get(name).getAsInt() : fallback;
    }

    private static int[] ints(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsInt();
        return result;
    }

    private static Vector3f vector(JsonArray array, float x, float y, float z) {
        return array == null ? new Vector3f(x, y, z)
                : new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static Quaternionf quaternion(JsonArray array) {
        return array == null ? new Quaternionf()
                : new Quaternionf(array.get(0).getAsFloat(), array.get(1).getAsFloat(),
                        array.get(2).getAsFloat(), array.get(3).getAsFloat());
    }

    private static int components(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> throw new IllegalArgumentException("Unsupported glTF accessor " + type);
        };
    }

    private record BufferView(int offset, int stride) {}
    private record Accessor(int view, int offset, int count, int componentType, String type) {}
    private record Mesh(int positionAccessor, int normalAccessor, int uvAccessor, int indexAccessor) {}
    private record Node(int mesh, int[] children, Vector3f translation, Quaternionf rotation, Vector3f scale) {}
    private record Sampler(float[] times, float[] values) {}
    private record Animation(List<Channel> channels, float duration) {}

    private record Channel(int node, String path, Sampler sampler) {
        void apply(float time, Vector3f[] translations, Quaternionf[] rotations, Vector3f[] scales) {
            int right = 0;
            while (right < sampler.times.length && sampler.times[right] < time) right++;
            if (right <= 0) right = 0;
            int left = Math.max(0, right - 1);
            right = Math.min(right, sampler.times.length - 1);
            float span = sampler.times[right] - sampler.times[left];
            float alpha = span <= 1.0E-6F ? 0.0F : (time - sampler.times[left]) / span;
            int width = path.equals("rotation") ? 4 : 3;
            if (path.equals("rotation")) {
                Quaternionf from = quaternion(left, sampler.values);
                Quaternionf to = quaternion(right, sampler.values);
                rotations[node].set(from).slerp(to, alpha);
            } else {
                Vector3f from = vector(left, width, sampler.values);
                Vector3f to = vector(right, width, sampler.values);
                Vector3f result = from.lerp(to, alpha);
                if (path.equals("translation")) translations[node].set(result);
                else if (path.equals("scale")) scales[node].set(result);
            }
        }

        private static Quaternionf quaternion(int index, float[] values) {
            int at = index * 4;
            return new Quaternionf(values[at], values[at + 1], values[at + 2], values[at + 3]);
        }

        private static Vector3f vector(int index, int width, float[] values) {
            int at = index * width;
            return new Vector3f(values[at], values[at + 1], values[at + 2]);
        }
    }
}
