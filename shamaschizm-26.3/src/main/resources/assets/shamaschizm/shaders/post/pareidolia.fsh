#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D FieldSampler;
uniform sampler2D ShadeSampler;
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
vec2 decode(vec4 c) {
    vec4 b=floor(c*255.0+0.5);
    return (vec2(b.r*256.0+b.g,b.b*256.0+b.a)/65535.0-0.5)*0.24;
}
vec2 flowAt(sampler2D source, vec2 uv) {
    ivec2 size=textureSize(source,0);
    vec2 p=clamp(uv,vec2(0.0),vec2(1.0))*vec2(size)-0.5;
    ivec2 cell=ivec2(floor(p));vec2 f=fract(p);
    vec2 a=decode(texelFetch(source,clamp(cell,ivec2(0),size-1),0));
    vec2 b=decode(texelFetch(source,clamp(cell+ivec2(1,0),ivec2(0),size-1),0));
    vec2 c=decode(texelFetch(source,clamp(cell+ivec2(0,1),ivec2(0),size-1),0));
    vec2 d=decode(texelFetch(source,clamp(cell+ivec2(1,1),ivec2(0),size-1),0));
    return mix(mix(a,b,f.x),mix(c,d,f.x),f.y);
}
void main() {
    vec2 flow=flowAt(FieldSampler,texCoord);
    vec2 px=0.5/vec2(textureSize(InSampler,0));
    vec2 uv=clamp(texCoord+flow,px,vec2(1.0)-px);
    vec3 color=texture(InSampler,uv).rgb;
    vec2 shading=clamp(flowAt(ShadeSampler,texCoord)/0.12,0.0,1.0);
    color*=1.0-shading.x;
    // Scale relief by local light so black remains black and hues remain intact.
    color*=1.0+shading.y;
    fragColor=vec4(color,1.0);
}
