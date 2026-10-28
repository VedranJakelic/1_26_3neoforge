#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform Projection { mat4 ProjMat; };
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; vec4 PortalSize; };
layout(location=0) in vec3 Position;
layout(location=0) out vec4 nearPoint;
layout(location=1) out vec4 fartherPoint;
void main() {
    gl_Position=vec4(Position.xy,0.5,1.0);
    nearPoint=inverse(ProjMat*PortalMatrix)*vec4(Position.xy,1,1);
    fartherPoint=inverse(ProjMat*PortalMatrix)*vec4(Position.xy,0.5,1);
}
