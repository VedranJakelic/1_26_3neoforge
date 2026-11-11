#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D DepthSampler;
layout(std140) uniform SurfaceConfig { vec4 Inverse0; vec4 Inverse1; vec4 Inverse2; vec4 Inverse3; vec4 Camera; vec4 Config; vec4 Door0; vec4 Door1; vec4 Door2; vec4 Door3; vec4 Door4; vec4 Door5; vec4 Door6; vec4 Door7; vec4 Door8; vec4 Door9; vec4 Door10; vec4 Door11; vec4 Door12; vec4 Door13; vec4 Door14; vec4 Door15; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453); }
// Relief is procedural, while the eyelids retain the original surface texture.
vec2 planeToScreen(vec2 uv,vec2 shift) {
    vec2 dx=dFdx(uv),dy=dFdy(uv);
    float determinant=dx.x*dy.y-dx.y*dy.x;
    if(abs(determinant)<0.0000001) return vec2(0);
    vec2 pixels=vec2(shift.x*dy.y-shift.y*dy.x,dx.x*shift.y-dx.y*shift.x)/determinant;
    return pixels/vec2(textureSize(InSampler,0));
}
vec3 eyeOnPlane(vec2 uv,float plane,out vec2 displacement) {
    vec2 cell=floor(uv/1.45);
    float seed=hash(cell+plane*37.0);
    vec2 center=(cell+0.5+0.22*vec2(sin(seed*83.0),cos(seed*61.0)))*1.45;
    float radius=0.16+0.07*seed;
    vec2 p=(uv-center)/radius;
    float age=mod(Config.x+seed*19.0,19.0);
    float presence=smoothstep(0.0,0.65,age)*(1.0-smoothstep(4.5,5.0,age));
    presence*=smoothstep(0.25,0.40,seed);
    presence*=smoothstep(hash(cell+plane*91.0)-0.06,hash(cell+plane*91.0)+0.06,Camera.w);
    // Upper lid travels further; opening is slower than closing.
    float opening=smoothstep(0.8,1.9,age)*(1.0-smoothstep(3.35,3.95,age));
    float ends=1.0-smoothstep(0.92,1.06,abs(p.x));
    float curve=pow(max(0.0,1.0-p.x*p.x),0.65);
    float seam=0.025*curve+0.018*p.x;
    float py=p.y-seam-0.075*opening*curve;
    float lid=(0.012+0.27*opening)*curve;
    float aa=max(0.012,fwidth(p.y)*0.7);
    float edge=abs(py)-lid;
    float aperture=(1.0-smoothstep(-aa,aa,edge))*ends*opening;
    float crease=exp(-pow((edge-0.045)/max(0.035,aa),2.0))*ends;
    float bank=exp(-pow((edge-0.14)/0.14,2.0))*ends;
    float skirt=(1.0-smoothstep(1.05,1.65,length(p)));
    // Pull the actual material apart around the lid; no pasted rectangular sprite.
    vec2 push=vec2(-p.x*0.035,-sign(py)*0.15)*opening*bank*skirt*radius*presence;
    displacement=planeToScreen(uv,push);
    vec2 irisPos=p-vec2(0.025*sin(seed*17.0),0.035);
    float ir=length(irisPos),angle=atan(irisPos.y,irisPos.x);
    float iris=1.0-smoothstep(0.25,0.29,ir);
    float pupil=1.0-smoothstep(0.090,0.116,ir);
    // Multiple integer angular frequencies: irregular radial fibers with no seam.
    float fibers=0.5+0.22*sin(angle*47.0+ir*56.0+sin(angle*13.0)*2.0)
                     +0.16*sin(angle*79.0-ir*39.0)+0.10*cos(angle*23.0+ir*83.0);
    float irisRim=exp(-pow((ir-0.267)/0.022,2.0));
    float upperShadow=aperture*(0.18+0.30*smoothstep(-0.12,0.24,py));
    float socket=aperture*0.10+crease*0.48;
    float dark=socket+upperShadow+aperture*(iris*(0.18+fibers*0.22)+pupil*0.72+irisRim*0.25);
    // Rounded lower bank catches a little light; texture remains visible on it.
    float relief=bank*(py<0.0?0.19:0.05)+aperture*(1.0-iris)*0.24;
    float glint=exp(-dot(irisPos-vec2(-0.065,0.085),irisPos-vec2(-0.065,0.085))/0.00038)
               *aperture*0.20;
    return vec3(clamp(dark,0.0,0.97),relief,glint)*presence;
}
bool portalWindow(vec3 relative) {
    vec4 doors[16]=vec4[16](Door0,Door1,Door2,Door3,Door4,Door5,Door6,Door7,Door8,Door9,Door10,Door11,Door12,Door13,Door14,Door15);
    for(int i=0;i<8;i++) {
        if(float(i)>=Config.z) break;
        vec4 origin=doors[i*2],right=doors[i*2+1];
        vec3 n=vec3(-right.z,0,right.x);
        float den=dot(relative,n);
        if(abs(den)<0.00001) continue;
        float f=dot(origin.xyz,n)/den;
        vec3 hit=relative*f-origin.xyz;
        if(f>0.0 && f<1.02 && abs(dot(hit,right.xyz))<right.w*0.5+0.04
           && hit.y>-.04 && hit.y<origin.w+.04) return true;
    }
    return false;
}
void main() {
    vec3 color=texture(InSampler,texCoord).rgb;
    float depth=texture(DepthSampler,texCoord).r;
    if(depth>=0.999999 || depth<=0.0) { fragColor=vec4(color,1);return; }
    mat4 inverse=mat4(Inverse0,Inverse1,Inverse2,Inverse3);
    vec4 h=inverse*vec4(texCoord*2.0-1.0,mix(depth*2.0-1.0,depth,Config.w),1);
    vec3 relative=h.xyz/h.w;
    float distance=length(relative);
    vec3 world=relative+Camera.xyz;
    // Derivative normals restrict projection to real visible surfaces, including models.
    vec3 normal=normalize(cross(dFdx(relative),dFdy(relative)));
    vec3 weights=pow(abs(normal),vec3(12.0)); weights/=max(dot(weights,vec3(1)),0.0001);
    vec2 warpX,warpY,warpZ;
    vec3 anatomy=eyeOnPlane(world.zy,1.0,warpX)*weights.x
                +eyeOnPlane(world.xz,2.0,warpY)*weights.y
                +eyeOnPlane(world.xy,3.0,warpZ)*weights.z;
    if(portalWindow(relative)) { fragColor=vec4(color,1);return; }
    float gain=Config.y*smoothstep(0.8,1.3,distance)*(1.0-smoothstep(16.0,22.0,distance));
    vec2 warp=clamp((warpX*weights.x+warpY*weights.y+warpZ*weights.z)*gain,vec2(-0.012),vec2(0.012));
    vec2 sampleUV=clamp(texCoord+warp,vec2(0.0),vec2(1.0));
    float sampleDepth=texture(DepthSampler,sampleUV).r;
    vec4 sampleH=inverse*vec4(sampleUV*2.0-1.0,mix(sampleDepth*2.0-1.0,sampleDepth,Config.w),1);
    vec3 samplePosition=sampleH.xyz/sampleH.w;
    // Do not stretch another object, sky, or a portal window across this surface.
    bool sameSurface=sampleDepth>0.0 && sampleDepth<0.999999
        && abs(dot(samplePosition-relative,normal))<0.07
        && length(samplePosition-relative)<0.15+distance*0.08;
    if(sameSurface && !portalWindow(samplePosition)) color=texture(InSampler,sampleUV).rgb;
    vec3 material=color;
    color*=1.0-clamp(anatomy.x*gain,0.0,0.97);
    color*=1.0+anatomy.y*gain;
    // A small reflection inherits local brightness rather than glowing in black.
    color+=material*anatomy.z*gain;
    fragColor=vec4(color,1);
}
