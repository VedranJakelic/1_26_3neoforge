#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform WisdomConfig { vec4 Lens; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
float hash(vec2 p) {
    vec3 q=fract(vec3(p.xyx)*0.1031);
    q+=dot(q,q.yzx+33.33);
    return fract((q.x+q.y)*q.z);
}
float shadowWeight(vec3 color) {
    float brightness=dot(color,vec3(0.2126,0.7152,0.0722));
    // Cubic falloff isolates strong interference to the deepest shadows.
    float darkness=1.0-smoothstep(0.02,0.50,brightness);
    return darkness*darkness*darkness;
}
float colorSelection(vec3 color) {
    float hi=max(color.r,max(color.g,color.b));
    float lo=min(color.r,min(color.g,color.b)),chroma=hi-lo;
    if(chroma<0.0001) return 0.0;
    float hue;
    if(hi==color.r) hue=(color.g-color.b)/chroma;
    else if(hi==color.g) hue=2.0+(color.b-color.r)/chroma;
    else hue=4.0+(color.r-color.g)/chroma;
    hue=fract(hue/6.0+1.0);
    float greenYellow=smoothstep(0.06,0.13,hue)*(1.0-smoothstep(0.36,0.46,hue));
    float blue=smoothstep(0.52,0.62,hue)*(1.0-smoothstep(0.71,0.78,hue));
    float saturation=chroma/max(hi,0.0001);
    return max(greenYellow,0.35*blue)*smoothstep(0.02,0.12,saturation);
}
void main() {
    float strength=clamp(Lens.x,0.0,1.0),time=Lens.y;
    vec2 size=vec2(textureSize(InSampler,0)),halfPixel=0.5/size;
    vec2 uv=clamp(texCoord,halfPixel,1.0-halfPixel);
    // Shadow selection follows scene content, never a screen-space pattern.
    float mask=shadowWeight(texture(InSampler,uv).rgb);
    float frame=floor(time*24.0);
    float grain=hash(floor(uv*size)+vec2(frame*19.0,7.0))-0.5;
    float row=hash(vec2(floor(uv.y*size.y*0.5),floor(time*18.0)))-0.5;
    // Random television jitter, without a repeating sine-wave distortion field.
    vec2 warped=clamp(uv+vec2(strength*row*mask*0.009,0.0),halfPixel,1.0-halfPixel);
    // Sample all channels together: no rainbow palette or colored channel separation.
    vec3 scene=texture(InSampler,warped).rgb;
    float gray=dot(scene,vec3(0.2126,0.7152,0.0722));
    // Green/yellow lose up to 90% saturation; blues up to 31.5%.
    float selection=colorSelection(scene);
    // Protect bright edges on either side of the jittered sample.
    float fuzzMask=min(mask,shadowWeight(scene));
    scene=mix(scene,vec3(gray),0.90*strength*selection);
    float fuzz=grain*1.40;
    scene+=vec3(strength*fuzzMask*fuzz);
    fragColor=vec4(clamp(scene,0.0,1.0),1.0);
}
