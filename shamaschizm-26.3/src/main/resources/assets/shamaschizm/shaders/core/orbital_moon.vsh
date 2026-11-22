#version 330
#extension GL_ARB_separate_shader_objects : require
#ifndef MINECRAFT_DYNAMIC_TRANSFORMS_GLSL
#define MINECRAFT_DYNAMIC_TRANSFORMS_GLSL

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    mat4 TextureMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
};

#endif
#ifndef MINECRAFT_PROJECTION_GLSL
#define MINECRAFT_PROJECTION_GLSL

layout(std140) uniform Projection {
    mat4 ProjMat;
};

vec4 projection_from_position(vec4 position) {
    vec4 projection = position * 0.5;
    projection.xy = vec2(projection.x + projection.w, projection.y + projection.w);
    projection.zw = position.zw;
    return projection;
}

#endif
layout(location=0) in vec3 Position;
layout(location=1) in vec2 UV0;
layout(location=2) in vec4 Color;
layout(location=0) out vec2 disc;
layout(location=1) out vec2 uv;
layout(location=2) out vec3 atmosphere;
void main(){
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);
    disc=Position.xy;uv=UV0;atmosphere=Color.rgb;
}
