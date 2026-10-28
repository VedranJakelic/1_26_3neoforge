#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; };
layout(std140) uniform EntityMaterial { vec4 Material; vec4 Detail; };
uniform sampler2D Sampler0;
layout(location=0) in vec2 texCoord;
layout(location=1) in vec4 tint;
layout(location=2) in vec4 lightColor;
layout(location=3) in vec4 overlayColor;
layout(location=4) in float portalDepth;
layout(location=0) out vec4 fragColor;
void main() {
    if(portalDepth<0.0||portalDepth>PortalPlane.w)discard;
    vec4 sampled=texture(Sampler0,texCoord);
    if(Detail.x>0.5)sampled=sampled.rrrr;
    vec4 color=sampled*tint;
    if(color.a<Material.w)discard;
    color.rgb=mix(overlayColor.rgb,color.rgb,overlayColor.a);
    fragColor=color*lightColor;
}
