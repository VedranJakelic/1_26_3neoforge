#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform FractalConfig { vec4 Fractal; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec3 scene=texture(InSampler,texCoord).rgb;
    float t=Fractal.x, strength=Fractal.y, danger=Fractal.z;
    vec2 size=vec2(textureSize(InSampler,0));
    vec2 p=(texCoord-0.5)*vec2(size.x/size.y,1.0)*2.0;
    p+=0.06*vec2(sin(t*0.09),cos(t*0.07));
    float radius=max(length(p),0.008);
    float angle=atan(p.y,p.x)+Fractal.w;
    // Logarithmic radial repetition gives nested petals that continuously drift inward.
    float radial=log(radius)*5.5+t*0.22;
    float petals=sin(angle*18.0+sin(radial*0.6+t*0.11)*1.2);
    float phase=radial*3.0+petals*2.4;
    float antiAlias=max(1.0,fwidth(phase)*1.3);
    float bands=sin(phase)/antiAlias;
    // Recursive mirrored folds add smaller self-similar filigree between the petals.
    vec2 q=vec2(radial*0.26,angle*2.8648);
    float detail=0.0, weight=0.5;
    for (int i=0;i<4;i++) {
        q=abs(fract(q+0.06*vec2(sin(t*0.06),cos(t*0.08)))-0.5)*2.0;
        float edge=min(abs(q.x-q.y),abs(q.x+q.y-1.0));
        detail+=weight*exp(-edge*18.0);
        q=q*1.8+vec2(0.17,0.31);
        weight*=0.5;
    }
    float hue=phase*0.7+t*0.12+detail*2.5;
    vec3 rainbow=0.5+0.5*cos(hue+vec3(0.0,2.0944,4.1888));
    vec3 pattern=rainbow*(0.32+0.45*(0.5+0.5*bands))+detail*rainbow*0.25;
    float luminance=dot(scene,vec3(0.2126,0.7152,0.0722));
    // At full health there is no blanket overlay: only genuinely dark pixels reveal it.
    float shadow=1.0-smoothstep(0.015,0.20,luminance);
    float shadowOpacity=0.90*shadow*shadow*shadow;
    // Reserve broad screen coverage for low health, reaching full coverage at two hearts.
    float takeover=smoothstep(0.45,1.0,clamp(danger,0.0,1.0));
    takeover*=takeover;
    float opacity=strength*mix(shadowOpacity,1.0,takeover);
    fragColor=vec4(mix(scene,clamp(pattern,0.0,1.0),opacity),1.0);
}
