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
uniform sampler2D Sampler0;
layout(location=0) in vec2 disc;
layout(location=1) in vec2 uv;
layout(location=2) in vec3 atmosphere;
layout(location=0) out vec4 fragColor;
void main(){
    // Vanilla's 8x8 square body sits inside its 32x32 glow sprite.
    bool body=max(abs(disc.x),abs(disc.y))<=0.25;
    bool glow=ColorModulator.x>0.5;
    if(body==glow)discard;
    vec3 artwork=texture(Sampler0,uv).rgb;
    // The unlit square occludes the Sun; the outer glow remains additive.
    fragColor=vec4(glow?artwork:atmosphere+artwork,ColorModulator.a);
}
