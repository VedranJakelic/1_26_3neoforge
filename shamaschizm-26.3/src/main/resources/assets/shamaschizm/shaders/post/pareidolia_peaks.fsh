#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main() {
    vec4 d=texture(InSampler,texCoord);
    vec2 px=1.0/vec2(textureSize(InSampler,0));
    // Suppress nearby competing centers before making any eye-shaped flow.
    float reach=max(2.0,d.g*0.12/px.y*0.65);
    for(int y=-3;y<=3;++y) for(int x=-3;x<=3;++x) {
        if(x==0 && y==0) continue;
        vec2 off=vec2(x,y)*reach/3.0*px;
        float other=texture(InSampler,texCoord+off).r;
        if(other>d.r+0.06) { fragColor=vec4(0);return; }
    }
    fragColor=d;
}
