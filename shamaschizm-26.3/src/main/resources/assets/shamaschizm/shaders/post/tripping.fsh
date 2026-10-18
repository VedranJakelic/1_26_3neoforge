#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D HistorySampler;
layout(std140) uniform TrippingConfig { vec4 Trip; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 rgbToHsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b,c.g));
    vec4 q = mix(vec4(p.xyw,c.r), vec4(c.r,p.yzx), step(p.x,c.r));
    float d = q.x - min(q.w,q.y);
    return vec3(abs(q.z + (q.w-q.y)/(6.0*d+1e-10)), d/(q.x+1e-10), q.x);
}
vec3 hsvToRgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(0.0,2.0/3.0,1.0/3.0))*6.0-3.0);
    return c.z * mix(vec3(1), clamp(p-1.0,0.0,1.0), c.y);
}
vec2 safeUV(vec2 uv, vec2 px) { return clamp(uv, px*0.5, vec2(1)-px*0.5); }
void main() {
    float t=Trip.x, strength=Trip.y, movement=Trip.z;
    vec2 size=vec2(textureSize(InSampler,0));
    vec2 px=1.0/size;
    vec2 p=(texCoord-0.5)*vec2(size.x/size.y,1.0);
    // Smooth breathing and intersecting waves; no camera rotation or aim changes.
    vec2 wave=vec2(sin(p.y*13.0+t*0.72)+sin(p.x*7.0-t*0.43),
                   sin(p.x*12.0+t*0.61)+cos(p.y*8.0+t*0.37));
    vec2 uv=texCoord + strength*(wave*0.0017 + (texCoord-0.5)*sin(t*0.85)*0.006);
    uv=safeUV(uv,px);
    vec3 base=texture(InSampler,uv).rgb;
    // A few pixels of colored edge separation, strengthened by walking/turning.
    vec2 split=px*(0.3+2.2*movement)*strength*vec2(cos(t*0.4),sin(t*0.3));
    base.r=texture(InSampler,safeUV(uv+split,px)).r;
    base.b=texture(InSampler,safeUV(uv-split,px)).b;
    float slow=0.12*sin(t*0.105);
    float motionHue=0.24*movement*sin(t*1.15 + p.y*0.7);
    vec3 hsv=rgbToHsv(base);
    hsv.x=fract(hsv.x+strength*(slow+motionHue));
    hsv.y=clamp(hsv.y*(1.0+0.20*strength),0.0,1.0);
    vec3 color=hsvToRgb(hsv);
    // A gentle changing tint also reaches neutral stone, without lighting up black areas.
    vec3 tint=0.5+0.5*cos(vec3(0,2.094,4.189)+t*0.19+movement*sin(t)*1.7);
    color*=mix(vec3(1),0.75+0.50*tint,strength*(0.35+0.65*movement));
    // Fine, drifting three-axis lattice, concentrated in readable mid-tones.
    vec2 q=p*31.0+vec2(sin(t*0.11),cos(t*0.13))*1.3;
    float lattice=(sin(q.x+t*0.2)*sin(dot(q,vec2(0.5,0.866))-t*0.16)
                   *sin(dot(q,vec2(-0.5,0.866))+t*0.12));
    float light=dot(base,vec3(0.2126,0.7152,0.0722));
    float patternMask=smoothstep(0.04,0.25,light)*(1.0-smoothstep(0.75,1.0,light));
    color+=strength*0.035*lattice*patternMask*(0.4+tint);
    vec3 history=texture(HistorySampler,texCoord).rgb;
    color=mix(color,history,clamp(Trip.w,0.0,0.85));
    fragColor=vec4(clamp(color,0.0,1.0),1.0);
}
