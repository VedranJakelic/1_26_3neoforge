#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform Projection { mat4 ProjMat; };
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; };
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=2) in vec2 UV0;
layout(location=3) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location=0) out vec2 texCoord;
layout(location=1) out vec4 tint;
layout(location=2) out float portalDepth;
void main() {
    gl_Position=ProjMat*PortalMatrix*vec4(Position,1.0);
    texCoord=UV0;
    tint=Color*texture(Sampler2,clamp(vec2(UV2)/256.0+vec2(0.5/16.0),vec2(0.5/16.0),vec2(15.5/16.0)));
    portalDepth=dot(PortalPlane.xyz,Position);
}
