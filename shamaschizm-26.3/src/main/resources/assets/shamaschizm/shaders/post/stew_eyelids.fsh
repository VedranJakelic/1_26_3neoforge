#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform EyelidConfig { vec4 Eyelid; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 sampleScene(vec2 uv, vec2 edge) {
    return texture(InSampler, clamp(uv, edge, vec2(1.0) - edge)).rgb;
}

void main() {
    // 0 = open, 1 = completely shut. A curved, pointed almond-shaped
    // aperture replaces the previous horizontal letterbox bars.
    float closed = smoothstep(0.0, 1.0, clamp(Eyelid.x, 0.0, 1.0));
    float x = abs(texCoord.x * 2.0 - 1.0);
    float almond = pow(max(0.0, 1.0 - pow(x, 2.25)), 0.64);
    float shaped = smoothstep(0.02, 0.72, closed);
    float halfOpening = (1.0 - closed) * 0.64 * mix(1.0, almond, shaped);

    // Negative within the open eye, positive behind the eyelids.
    float boundary = abs(texCoord.y - 0.5) - halfOpening;
    float feather = 0.012 + 0.020 * closed;
    float lid = smoothstep(-feather, feather, boundary);
    lid = max(lid, smoothstep(0.995, 1.0, closed));

    // Blur the scene progressively as the eye closes, including the HUD.
    vec2 px = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 edge = 0.5 * px;
    vec2 r = px * (0.5 + 10.0 * closed);
    vec3 original = sampleScene(texCoord, edge);
    vec3 axial = sampleScene(texCoord + vec2( r.x, 0.0), edge)
               + sampleScene(texCoord + vec2(-r.x, 0.0), edge)
               + sampleScene(texCoord + vec2(0.0,  r.y), edge)
               + sampleScene(texCoord + vec2(0.0, -r.y), edge);
    vec3 diagonal = sampleScene(texCoord + vec2( r.x,  r.y), edge)
                  + sampleScene(texCoord + vec2(-r.x,  r.y), edge)
                  + sampleScene(texCoord + vec2( r.x, -r.y), edge)
                  + sampleScene(texCoord + vec2(-r.x, -r.y), edge);
    vec3 soft = original * 0.28 + axial * 0.12 + diagonal * 0.06;
    vec3 view = mix(original, soft, 0.85 * closed);
    fragColor = vec4(view * (1.0 - lid), 1.0);
}
