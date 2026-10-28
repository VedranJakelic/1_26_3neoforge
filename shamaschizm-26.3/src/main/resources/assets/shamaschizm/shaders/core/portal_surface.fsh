#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform Projection { mat4 ProjMat; };
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; vec4 PortalSize; };
uniform sampler2D Sampler0;
layout(location=0) in vec4 nearPoint;
layout(location=1) in vec4 fartherPoint;
layout(location=0) out vec4 fragColor;
void main() {
    if(PortalPlane.w < 0.5) {
        vec3 start=nearPoint.xyz/nearPoint.w;
        vec3 ray=fartherPoint.xyz/fartherPoint.w-start;
        float denominator=dot(ray,PortalPlane.xyz);
        if(abs(denominator)<0.0000001) discard;
        // Both points include projection-space bob/hurt translations. A reconstructed
        // unbobbled eye would distort the aperture during sideways movement.
        float t=-dot(start,PortalPlane.xyz)/denominator;
        vec3 hit=start+t*ray;
        vec3 right=vec3(PortalPlane.z,0,-PortalPlane.x);
        if(abs(dot(hit,right))>PortalSize.x*0.5 || hit.y<0.0 || hit.y>PortalSize.y) discard;
        vec4 clip=ProjMat*PortalMatrix*vec4(hit,1);
        if(clip.w<=0.0) discard;
        gl_FragDepth=clamp(clip.z/clip.w,0.0,0.999999);
    } else {
        // While awaiting the server teleport, show the mapped destination rather than local terrain.
        gl_FragDepth=0.999999;
    }
    ivec2 pixel=clamp(ivec2(gl_FragCoord.xy),ivec2(0),textureSize(Sampler0,0)-ivec2(1));
    fragColor=vec4(texelFetch(Sampler0,pixel,0).rgb,1.0);
}
