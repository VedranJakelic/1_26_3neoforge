#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D AstralEyesSampler;
layout(std140) uniform AstralConfig { vec4 Astral; };
layout(std140) uniform AstralSun { vec4 Sun; };
layout(std140) uniform AstralMoon { vec4 Moon; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
mat2 rotate(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
vec2 flow(vec2 p,float t){
 p+=.10*vec2(sin(p.y*3.1+t*.15),cos(p.x*2.7-t*.12));
 p+=.045*vec2(sin(p.y*6.0-t*.17),cos(p.x*5.2+t*.13));return p;
}
vec3 field(vec2 p,float t){
 vec2 q=p;float sum=0.0,weight=.55;
 for(int i=0;i<5;i++){q=rotate(.48+t*.008)*q;q=abs(q)/max(dot(q,q),.16)-vec2(.72,.86);sum+=exp(-8.0*abs(length(q)-.75))*weight;weight*=.65;}
 float ridges=.5+.5*cos(length(p)*15.0-atan(p.y,p.x)*5.0+t*.12);
 vec3 palette=.5+.5*cos(vec3(.3,2.4,4.6)+sum*4.0+length(p)*1.3-t*.055);
 return palette*(.12+sum*.7+ridges*.09);
}
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7)))*43758.5453);}
// Individual reference eyes follow irregular melting strands; no rings or circular chains.
vec3 eyes(vec3 bg,vec2 p,float age,float t){
 float amount=smoothstep(15.0,180.0,age);const float spacing=.65;vec2 cell=floor(p/spacing);
 for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++){
  vec2 id=cell+vec2(x,y);float seed=hash(id),size=mix(.10,.23,hash(id+7.3));
  vec2 jitter=vec2(hash(id+13.1),hash(id+29.7)),center=(id+.1+jitter*.8)*spacing;
  vec2 tangent=vec2(cos(center.y*2.3-t*.12)+.4*sin(center.x*3.1),sin(center.x*2.1+t*.10)+.4*cos(center.y*2.7));
  vec2 local=rotate(atan(tangent.y,tangent.x))*(p-center)/vec2(size,size*.66);
  float edge=1.0-smoothstep(.75,1.0,length(local));if(edge<=0.0)continue;
  float ridge=dot(field(center,t),vec3(.3333));
  float emergence=smoothstep(seed*.8,seed*.8+.2,amount)*smoothstep(.10,.25,ridge);
  vec2 uv=vec2(.448,.163)+vec2(local.x,-local.y)*vec2(.039,.017);
  vec3 art=texture(AstralEyesSampler,uv).rgb;float ink=smoothstep(.09,.25,max(art.r,max(art.g,art.b)));
  bg=mix(bg,art,edge*ink*emergence*.92);
 }
 return bg;
}
vec3 apparition(vec2 center,vec2 aspect,float age,float t){vec2 p=flow((texCoord-center)*aspect*3.0,t);return eyes(field(p,t),p,age,t);}
void main(){
 float t=Astral.x,age=Astral.z,danger=Astral.w;vec2 size=vec2(textureSize(InSampler,0)),aspect=vec2(size.x/size.y,1.0);
 vec2 uv=texCoord+danger*.025*vec2(sin(texCoord.y*23.0+t*.9),cos(texCoord.x*19.0-t*.8));
 vec3 scene=texture(InSampler,clamp(uv,vec2(.001),vec2(.999))).rgb;
 float forming=smoothstep(0.0,18.0,age);vec3 pattern=apparition(vec2(.5),aspect,age,t);
 float sw=Sun.z*(1.0-smoothstep(.9,2.5,length((Sun.xy-.5)*aspect)));
 float mw=Moon.z*(1.0-smoothstep(.9,2.5,length((Moon.xy-.5)*aspect)));
 if(sw>.001)pattern=mix(pattern,apparition(mix(vec2(.5),Sun.xy,forming),aspect,age,t),forming*sw);
 if(mw>.001)pattern=mix(pattern,apparition(mix(vec2(.5),Moon.xy,forming),aspect,age,t+2.7),forming*mw);
 float darkness=1.0-smoothstep(.02,.45,dot(scene,vec3(.2126,.7152,.0722)));
 vec3 color=mix(scene,clamp(pattern,0.0,1.0),Astral.y*(.20+.65*darkness));
 color*=1.0+danger*danger*3.0;color=mix(color,vec3(1.0,.98,.88),smoothstep(.65,1.0,danger)*.88);
 fragColor=vec4(clamp(color,0.0,1.0),1.0);
}
