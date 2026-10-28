#version 330
#extension GL_ARB_separate_shader_objects : require
layout(std140) uniform Projection { mat4 ProjMat; };
layout(std140) uniform PortalView { mat4 PortalMatrix; vec4 PortalPlane; };
layout(std140) uniform EntityMaterial { vec4 Material; vec4 Detail; };
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=2) in vec2 UV0;
layout(location=3) in ivec2 UV1;
layout(location=4) in ivec2 UV2;
layout(location=5) in vec3 Normal;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
layout(location=0) out vec2 texCoord;
layout(location=1) out vec4 tint;
layout(location=2) out vec4 lightColor;
layout(location=3) out vec4 overlayColor;
layout(location=4) out float portalDepth;
void main() {
    gl_Position=ProjMat*PortalMatrix*vec4(Position,1);
    float shade=min(1.0,0.4+0.6*(max(0.0,dot(normalize(vec3(0.2,1,-0.7)),Normal))+max(0.0,dot(normalize(vec3(-0.2,1,0.7)),Normal))));
    tint=vec4(Color.rgb*mix(1.0,shade,Material.x),Color.a);
    lightColor=mix(texture(Sampler2,clamp(vec2(UV2)/256.0+vec2(0.5/16.0),vec2(0.5/16.0),vec2(15.5/16.0))),vec4(1),Material.y);
    overlayColor=Material.z>0.5?texelFetch(Sampler1,clamp(UV1,ivec2(0),ivec2(15)),0):vec4(0,0,0,1);
    texCoord=UV0;
    portalDepth=dot(PortalPlane.xyz,Position);
}
