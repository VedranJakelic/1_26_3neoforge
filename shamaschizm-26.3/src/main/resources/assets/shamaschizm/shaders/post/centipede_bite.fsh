#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D HistorySampler;
layout(std140) uniform BiteConfig { vec4 Bite; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main(){
 vec2 halfPixel=0.5/vec2(textureSize(InSampler,0));vec3 color=vec3(0.0);
 for(int i=0;i<9;i++){
  float zoom=float(i)/8.0*0.11*Bite.x;
  vec2 uv=clamp(texCoord-(texCoord-0.5)*zoom,halfPixel,vec2(1.0)-halfPixel);
  color+=texture(InSampler,uv).rgb/9.0;
 }
 if(Bite.y>0.0)color=mix(color,texture(HistorySampler,texCoord).rgb,Bite.y);
 fragColor=vec4(color,1.0);
}
