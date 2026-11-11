#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D TargetSampler;
uniform sampler2D HistorySampler;
uniform sampler2D SceneSampler;
uniform sampler2D PreviousSampler;
layout(std140) uniform EyeConfig { vec4 Eye; };
layout(std140) uniform EyeHistory { vec4 History; };
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
    vec2 px=1.0/vec2(textureSize(SceneSampler,0));
    vec2 target=flowAt(TargetSampler,texCoord);
    // Light spatial regularization prevents competing candidate seams becoming tears.
    target=(target*4.0+flowAt(TargetSampler,texCoord+vec2(px.x,0))
            +flowAt(TargetSampler,texCoord-vec2(px.x,0))
            +flowAt(TargetSampler,texCoord+vec2(0,px.y))
            +flowAt(TargetSampler,texCoord-vec2(0,px.y)))/8.0;
    vec2 previous=vec2(0.0);
    if(History.x>0.5) {
        // Small image-based motion match: the existing bend follows slow camera
        // motion instead of being cleared whenever the player moves. No depth or
        // camera changes, so the same logic also covers composited portal views.
        float best=1e10;vec2 offset=vec2(0.0);
        for(int y=-2;y<=2;++y) for(int x=-2;x<=2;++x) {
            vec2 shift=vec2(x,y)*px;float error=0.00015*float(x*x+y*y);
            for(int k=0;k<5;++k) {
                vec2 patch=k==0?vec2(0):k==1?vec2(px.x,0):k==2?vec2(-px.x,0):k==3?vec2(0,px.y):vec2(0,-px.y);
                vec3 delta=texture(SceneSampler,texCoord+patch).rgb-texture(PreviousSampler,texCoord+patch+shift).rgb;
                error+=dot(delta,delta);
            }
            if(error<best) {best=error;offset=shift;}
        }
        previous=flowAt(HistorySampler,texCoord+offset);
    }
    // About 2 seconds to reach 90%: actual deformation builds, not just a tint.
    // Rapid movement relaxes more quickly but never toggles the warp on/off.
    float tau=mix(0.9,0.40,History.y);
    float alpha=1.0-exp(-Eye.w/tau);
    vec2 flow=mix(previous,target,alpha);
    vec2 encoded=floor(clamp(flow/0.24+0.5,0.0,1.0)*65535.0+0.5);
    fragColor=vec4(floor(encoded.x/256.0),mod(encoded.x,256.0),floor(encoded.y/256.0),mod(encoded.y,256.0))/255.0;
}
