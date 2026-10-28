#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; };
uniform sampler2D Sampler0;
layout(location=0) in vec2 texCoord;
layout(location=1) in vec4 tint;
layout(location=2) in float portalDepth;
layout(location=0) out vec4 fragColor;
void main() {
    if(portalDepth < 0.0 || portalDepth > PortalPlane.w) discard;
    vec4 color=texture(Sampler0,texCoord)*tint;
    if(color.a < 0.003) discard;
    fragColor=color;
}
