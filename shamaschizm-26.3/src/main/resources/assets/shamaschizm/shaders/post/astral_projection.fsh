#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D AstralEyesSampler;
layout(std140) uniform AstralConfig { vec4 Astral; };
layout(std140) uniform AstralSun { vec4 Sun; };
layout(std140) uniform AstralMoon { vec4 Moon; };
layout(std140) uniform AstralInverse0 { vec4 Inverse0; };
layout(std140) uniform AstralInverse1 { vec4 Inverse1; };
layout(std140) uniform AstralInverse2 { vec4 Inverse2; };
layout(std140) uniform AstralInverse3 { vec4 Inverse3; };
layout(std140) uniform AstralSunDirection { vec4 SunDirection; };
layout(std140) uniform AstralMoonDirection { vec4 MoonDirection; };
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
// Trace the exact spiral phase used by field(): 15*r - 5*angle.
// Each eye and its smaller successors bend along the same phase contour.
vec3 eyes(vec3 bg,vec2 p,float age,float t,float spacing,float sizeScale){
 float amount=smoothstep(5.0,15.0,age)*mix(.22,1.0,smoothstep(15.0,180.0,age));
 const float spiral=5.0/15.0;
 vec2 cell=floor(p/spacing);float radius=length(p),angle=atan(p.y,p.x);
 vec3 strand=bg;float pixelWidth=max(length(fwidth(p)),.0001);
 for(int y=-2;y<=2;y++)for(int x=-2;x<=2;x++){
  vec2 id=cell+vec2(x,y);float seed=hash(id);
  vec2 jitter=vec2(hash(id+13.1),hash(id+29.7)),center=(id+.1+jitter*.8)*spacing;
  if(length(p-center)>.85*sizeScale)continue;
  float cr=length(center);if(cr<.15*sizeScale)continue;
  float ca=atan(center.y,center.x),eyeSize=mix(.065,.16,pow(hash(id+7.3),1.4))*sizeScale;
  float emergence=smoothstep(seed*.8,seed*.8+.2,amount);
  if(emergence<=0.0)continue;
  for(int child=0;child<4;child++){
   float da=atan(sin(angle-ca),cos(angle-ca));
   float arc=sqrt(cr*cr+spiral*spiral);
   vec2 local=vec2(da*arc/(eyeSize*1.3),(radius-cr-spiral*da)/(eyeSize*.48));
   float edge=1.0-smoothstep(.35,1.0,length(local));
   // Subpixel descendants fade away instead of shimmering during camera turns.
   float pixels=eyeSize/pixelWidth;
   float detail=smoothstep(1.5,4.0,pixels);
   if(edge>0.0&&detail>0.0){
    vec2 uv=vec2(.448,.163)+vec2(local.x,-local.y)*vec2(.039,.017);
    vec3 art=texture(AstralEyesSampler,uv).rgb;
    float ink=smoothstep(.09,.25,max(art.r,max(art.g,art.b)));
    float luminance=dot(art,vec3(.2126,.7152,.0722));
    // Only reference light/dark detail survives; hue comes entirely from the strand.
    vec3 woven=strand*(.22+2.6*luminance);
    float smaller=child==0?1.0:smoothstep(.08,.65,amount);
    bg=mix(bg,woven,edge*ink*emergence*detail*smaller*.92);
   }
   float stepAngle=eyeSize*2.15/arc;
   ca+=stepAngle;cr+=spiral*stepAngle;eyeSize*=.55;
  }
 }
 return bg;
}
vec3 apparition(vec3 ray,vec3 center,float age,float t){
 // Both celestial directions lie in the world XY plane. Z is a stable tangent,
 // avoiding camera-dependent rotation, stretching and crosshair recentering.
 vec3 tangent=vec3(0.0,0.0,1.0),up=normalize(cross(center,tangent));
 float forward=dot(ray,center);
 vec2 p=vec2(atan(dot(ray,tangent),forward),atan(dot(ray,up),forward))*2.5;
 p=flow(p,t);
 vec3 pattern=eyes(field(p,t),p,age,t,.43,1.0);
 // Add a finer population only near the celestial source; keep its original
 // spiral coordinates so the small eyes follow the same curves as the large ones.
 float close=smoothstep(.88,.985,forward);
 if(close>.001){
  vec3 fine=eyes(pattern,p,age,t,.19,.44);
  pattern=mix(pattern,fine,close);
 }
 return pattern;
}
// These are astral visions within the pattern, not physical sky geometry.
// Keep them visible underground, independent of the post-GUI depth buffer.
vec3 celestialGlow(vec3 color,vec4 light,vec2 aspect,vec3 tint,float strength){
 if(light.z<=0.0)return color;
 vec2 delta=(texCoord-light.xy)*aspect;
 float r2=dot(delta,delta);
 float core=exp(-r2/(2.0*.055*.055));
 float halo=exp(-r2/(2.0*.145*.145));
 float exposure=(12.0*core+.55*halo)*strength*light.z*Astral.y;
 // Exponential exposure hides the vanilla square disk inside the bright core,
 // with a continuous falloff into a broad, fully soft halo.
 return mix(color,tint,1.0-exp(-exposure));
}
void main(){
 float t=Astral.x,age=Astral.z,danger=Astral.w;vec2 size=vec2(textureSize(InSampler,0)),aspect=vec2(size.x/size.y,1.0);
 vec2 uv=texCoord+danger*.025*vec2(sin(texCoord.y*23.0+t*.9),cos(texCoord.x*19.0-t*.8));
 vec3 scene=texture(InSampler,clamp(uv,vec2(.001),vec2(.999))).rgb;
 // Only the two world-anchored celestial fields. No crosshair-centered fallback.
 float forming=smoothstep(0.0,18.0,age);
 vec4 world=mat4(Inverse0,Inverse1,Inverse2,Inverse3)*vec4(texCoord*2.0-1.0,1.0,1.0);
 vec3 ray=normalize(world.xyz);
 vec3 sunDirection=normalize(SunDirection.xyz),moonDirection=normalize(MoonDirection.xyz);
 // Per-direction weights stay fixed in the sky as the camera turns. Fade before
 // the tangent-plane seams; never blend an unrelated screen-centered pattern.
 float sw=smoothstep(.05,.45,dot(ray,sunDirection));
 float mw=smoothstep(.05,.45,dot(ray,moonDirection));
 vec3 pattern=vec3(0.0);
 if(sw>.001)pattern+=apparition(ray,sunDirection,age,t)*sw;
 if(mw>.001)pattern+=apparition(ray,moonDirection,age,t+2.7)*mw;
 pattern/=max(sw+mw,.001);
 float darkness=1.0-smoothstep(.02,.45,dot(scene,vec3(.2126,.7152,.0722)));
 float coverage=forming*clamp(sw+mw,0.0,1.0);
 // A local daylight boost follows angular distance, never the screen center.
 float close=smoothstep(.88,.985,max(dot(ray,sunDirection),dot(ray,moonDirection)));
 float strength=mix(.20+.45*darkness,.90,close);
 vec3 color=mix(scene,clamp(pattern,0.0,1.0),coverage*Astral.y*strength);
 color=celestialGlow(color,Sun,aspect,vec3(1.0,.975,.90),1.0);
 color=celestialGlow(color,Moon,aspect,vec3(.92,.96,1.0),.85);
 color*=1.0+danger*danger*3.0;color=mix(color,vec3(1.0,.98,.88),smoothstep(.65,1.0,danger)*.88);
 fragColor=vec4(clamp(color,0.0,1.0),1.0);
}
