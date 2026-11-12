#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D RitualSampler;
uniform sampler2D HomunculusRitualSampler;
uniform sampler2D HomunculusSealsSampler;
layout(std140) uniform FractalConfig { vec4 Fractal; };
layout(std140) uniform FractalEyes { vec4 Eyes; };
layout(std140) uniform TripVision { vec4 Vision; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

// The schematic retains its flowing ink reveal; seals render their original artwork.
vec3 revealDrawing(vec3 background, sampler2D drawing, vec2 center, vec2 aspect,
                   vec2 finalSize, vec2 sourceCenter, vec2 sourceSpan,
                   float progress, float phase, float visibility) {
    float growth=smoothstep(0.0,14.0/17.0,progress);
    float readable=smoothstep(2.0/17.0,1.0,progress);
    vec2 imageSize=finalSize*mix(.012/.86,1.0,growth);
    vec2 uv=(texCoord-center)*aspect/imageSize+vec2(.5);
    uv.y=1.0-uv.y;
    float inside=step(0.0,uv.x)*step(uv.x,1.0)*step(0.0,uv.y)*step(uv.y,1.0);
    vec2 wave=vec2(sin(uv.y*19.0+Fractal.x*.18),cos(uv.x*17.0-Fractal.x*.16));
    vec2 formingUV=uv+wave*.055*pow(1.0-readable,2.0);
    vec2 sampleUV=sourceCenter+(clamp(formingUV,vec2(0),vec2(1))-vec2(.5))*sourceSpan;
    vec4 art=texture(drawing,sampleUV);
    float coverage=max(art.r,max(art.g,art.b))*art.a;
    float lines=smoothstep(.10,.82,coverage)*inside;
    float emergence=smoothstep(0.0,1.0,progress)*visibility;
    vec2 imageP=(texCoord-center)*aspect/(imageSize*.62);
    float backdrop=(1.0-smoothstep(.65,1.2,length(imageP)))*emergence;
    background*=1.0-backdrop*.82;
    vec3 ink=mix(.5+.5*cos(vec3(0,2.0944,4.1888)+phase),vec3(.98),readable);
    return mix(background,ink,clamp(lines*emergence,0.0,1.0));
}
// Direct RGBA artwork: no coverage reconstruction, recoloring, or dark halo.
vec3 revealSymbol(vec3 background, vec2 center, vec2 aspect, vec2 sourceCenter,
                  float progress, float visibility, float spin) {
    float growth=smoothstep(0.0,14.0/17.0,progress);
    vec2 local=(texCoord-center)*aspect/(.36*mix(.012/.86,1.0,growth));
    float angle=spin*(1.0-smoothstep(0.0,1.0,progress));
    float c=cos(angle), s=sin(angle);
    local=mat2(c,-s,s,c)*local;
    vec2 uv=vec2(local.x,-local.y)+vec2(.5);
    float inside=step(0.0,uv.x)*step(uv.x,1.0)*step(0.0,uv.y)*step(uv.y,1.0);
    vec4 art=texture(HomunculusSealsSampler,sourceCenter+(clamp(uv,0.0,1.0)-.5)*vec2(.244,.240));
    // Color-key only the residual black background in the supplied texture.
    // Keep the actual RGB artwork, including its original grey linework.
    float nonBlack=smoothstep(0.0,.04,max(art.r,max(art.g,art.b)));
    float alpha=art.a*nonBlack*inside*smoothstep(0.0,1.0,progress)*visibility;
    return mix(background,art.rgb,clamp(alpha,0.0,1.0));
}
vec3 renderPattern(vec2 center, float pin, float visionAge, float visionOpacity) {
    float t=Fractal.x, strength=Fractal.y;
    vec2 size=vec2(textureSize(InSampler,0));
    vec2 aspect=vec2(size.x/size.y,1.0);
    vec2 p=(texCoord-center)*aspect*2.0;
    vec2 visionP=p;
    float opening=smoothstep(0.0,4.0,visionAge)*visionOpacity;
    float morph=smoothstep(9.0,16.0,visionAge);
    bool homunculus=Eyes.z>.5;
    float rise=homunculus?smoothstep(23.0,35.0,visionAge):smoothstep(16.0,23.0,visionAge);
    float reveal=smoothstep(23.0,40.0,visionAge);
    vec2 ce=visionP/.65;
    float centralSupport=1.0-smoothstep(.85,1.6,length(ce));
    float centralLid=max(.17,.42*sqrt(max(0.0,1.0-ce.x*ce.x)));
    p.y*=1.0+opening*(1.0-rise)*centralSupport*(1.0/centralLid-1.0)*.5;
    p+=0.06*vec2(sin(t*0.09),cos(t*0.07))*(1.0-pin);
    // Eyes and fractals live in the same gently flowing coordinate system.
    // Applying this before BOTH shape generation stages carries the eyes with it.
    p+=vec2(.045*(sin(p.y*3.2+t*.28)-pin*sin(t*.28)),.035*(sin(p.x*3.0-t*.24)+pin*sin(t*.24)));
    float ca=cos(Fractal.w),sa=sin(Fractal.w);
    p=mat2(ca,sa,-sa,ca)*p;
    // Stable, slowly drifting features in the fractal's own coordinates.
    // Deform its input coordinates BEFORE evaluating the pattern: the bands
    // themselves become lids and irises instead of a blurred post-process hint.
    vec2 originalP=p;
    vec2 totalWarp=vec2(0.0); float totalWeight=0.0, totalShade=0.0, totalRelief=0.0;
    for(int i=0;i<7;++i) {
        float fi=float(i);
        float angle=fi*2.399963+0.045*sin(t*0.055+fi);
        float ring=0.48+0.20*mod(fi,3.0);
        vec2 center=vec2(cos(angle),sin(angle))*ring;
        float r=0.20+0.035*sin(fi*1.7);
        vec2 local=(originalP-center)/r;
        float mask=1.0-smoothstep(1.05,1.65,length(local));
        {
            float eyeMask=mask; vec2 ep=local;
            // Gentle breathing, no periodic complete disappearance.
            float eyeAmount=0.75*Eyes.x*strength*(0.88+0.12*sin(t*0.16+fi))
                *smoothstep(fi/7.0,(fi+1.0)/7.0,Eyes.y);
            float lid=max(0.15,0.48*pow(max(0.0,1.0-local.x*local.x),0.65));
            vec2 warped=local;
            warped.y*=mix(1.0,1.0/lid,eyeAmount*mask);
            // Bend nearby bands into a circular iris around a dark pupil.
            float ir=length(local);
            warped+=local*eyeAmount*mask*(0.85*exp(-pow(ir/0.30,2.0))
                        -0.32*exp(-pow((ir-0.48)/0.16,2.0)));
            totalWarp+=(warped-local)*r; totalWeight+=mask;
    // Readable but palette-preserving eye anatomy, coherent with the warp.
    float shadeLid=0.48*pow(max(0.0,1.0-ep.x*ep.x),0.65);
    float aperture=(1.0-smoothstep(shadeLid-0.025,shadeLid+0.025,abs(ep.y)))
                  *(1.0-smoothstep(0.95,1.02,abs(ep.x)));
    float rim=exp(-pow((abs(ep.y)-shadeLid)/0.045,2.0))
              *(1.0-smoothstep(0.94,1.04,abs(ep.x)));
    float shadeIr=length(ep);
    float iris=1.0-smoothstep(0.31,0.36,shadeIr);
    float pupil=1.0-smoothstep(0.12,0.155,shadeIr);
    float irisEdge=exp(-pow((shadeIr-0.33)/0.025,2.0));
    float fibers=0.5+0.5*sin(atan(ep.y,ep.x)*31.0+shadeIr*36.0);
    float shade=clamp(rim*0.65+aperture*(pupil*0.88+irisEdge*0.55+iris*(0.12+fibers*0.16)),0.0,0.96);
    float anatomy=clamp(eyeAmount*eyeMask,0.0,1.0);
    totalShade+=anatomy*shade;
    totalRelief+=anatomy*aperture*(1.0-iris)*0.35;

        }
    }
    p=originalP+totalWarp/max(1.0,totalWeight);
    float radius=max(length(p),0.008);
    float angle=atan(p.y,p.x);
    // Logarithmic radial repetition gives nested petals that continuously drift inward.
    float radial=log(radius)*5.5+t*0.22;
    float petals=sin(angle*18.0+sin(radial*0.6+t*0.11)*1.2);
    float phase=radial*3.0+petals*2.4;
    float antiAlias=max(1.0,fwidth(phase)*1.3);
    float bands=sin(phase)/antiAlias;
    // Recursive mirrored folds add smaller self-similar filigree between the petals.
    vec2 q=vec2(radial*0.26+0.6*cos(angle*9.0),0.6*sin(angle*9.0));
    float detail=0.0, weight=0.5;
    for (int i=0;i<4;i++) {
        q=abs(fract(q+0.06*vec2(sin(t*0.06),cos(t*0.08)))-0.5)*2.0;
        float edge=min(abs(q.x-q.y),abs(q.x+q.y-1.0));
        detail+=weight*exp(-edge*18.0);
        q=q*1.8+vec2(0.17,0.31);
        weight*=0.5;
    }
    float hue=phase*0.7+t*0.12+detail*2.5;
    vec3 rainbow=0.5+0.5*cos(hue+vec3(0.0,2.0944,4.1888));
    vec3 pattern=rainbow*(0.32+0.45*(0.5+0.5*bands))+detail*rainbow*0.25;
    pattern*=1.0-clamp(totalShade/max(1.0,totalWeight),0.0,0.96);
    pattern*=1.0+totalRelief/max(1.0,totalWeight);
    // Central eye: lids open, then the iris morphs into the uploaded seal.
    float eyeHeight=.42*opening*sqrt(max(0.0,1.0-ce.x*ce.x));
    float centralAperture=(1.0-smoothstep(eyeHeight-.025,eyeHeight+.025,abs(ce.y)))
                        *(1.0-smoothstep(.94,1.03,abs(ce.x)))*opening;
    float centralRim=exp(-pow((abs(ce.y)-eyeHeight)/.04,2.0))*(1.0-smoothstep(.94,1.03,abs(ce.x)));
    float irisRadius=length(ce);
    float irisShade=(1.0-smoothstep(.27,.32,irisRadius))*.45+(1.0-smoothstep(.10,.14,irisRadius))*.50;
    pattern*=1.0-clamp((centralRim*.55+centralAperture*irisShade*(1.0-(homunculus?rise:morph)))*opening*(1.0-rise),0.0,.95);
    if(visionOpacity>0.0) {
    if (!homunculus) {
    // Match the rising seal exactly to its position in the final complete image.
    vec2 imageSize=vec2(929.0/780.0,1.0)*.80;
    vec2 imageCenter=center-vec2(0,.05);
    vec2 sealFinal=imageCenter+vec2((.506-.5)*imageSize.x/aspect.x,(.5-.231)*imageSize.y);
    vec2 sealCenter=mix(center,sealFinal,rise);
    float sealRadius=mix(.0975,.1385,rise);
    vec2 sealQ=(texCoord-sealCenter)*aspect/sealRadius;
    vec2 sealUV=vec2(.506,.231)+vec2(sealQ.x,-sealQ.y)*vec2(.147,.173);
    float seal=texture(RitualSampler,clamp(sealUV,vec2(0),vec2(1))).r;
    seal*=1.0-smoothstep(.98,1.06,length(sealQ));
    float sealAmount=morph*visionOpacity*(1.0-reveal);
    vec3 apparition=mix(.5+.5*cos(vec3(0,2.0944,4.1888)+phase),vec3(.96),morph);
    pattern=mix(pattern,apparition,clamp(seal*sealAmount,0.0,1.0));
    // Distorted colored fragments settle into the original, unmodified ritual.
    vec2 imageUV=(texCoord-imageCenter)*aspect/imageSize+vec2(.5);
    imageUV.y=1.0-imageUV.y;
    float inside=step(0.0,imageUV.x)*step(imageUV.x,1.0)*step(0.0,imageUV.y)*step(imageUV.y,1.0);
    vec2 wave=vec2(sin(imageUV.y*31.0+t*.24),cos(imageUV.x*27.0-t*.21));
    vec2 formingUV=imageUV+wave*.045*pow(1.0-reveal,2.0);
    // Continuous UV deformation; no pixel-grid quantization.
    float lines=texture(RitualSampler,clamp(formingUV,vec2(0),vec2(1))).r*inside;
    float emergence=reveal*visionOpacity;
    vec2 imageP=(texCoord-imageCenter)*aspect/(imageSize*.65);
    float backdrop=(1.0-smoothstep(.7,1.25,length(imageP)))*emergence;
    pattern*=1.0-backdrop*.72;
    vec3 ink=mix(.5+.5*cos(vec3(0,2.0944,4.1888)+phase),vec3(.98),reveal);
    pattern=mix(pattern,ink,clamp(lines*emergence,0.0,1.0));
    } else {
        vec2 sources[4]=vec2[4](vec2(.198,.178),vec2(.815,.176),vec2(.195,.818),vec2(.821,.816));
        for(int i=0;i<4;i++) {
            vec2 side=vec2((i==0||i==2)?-1.0:1.0,i<2?1.0:-1.0);
            vec2 offset=side*mix(vec2(.32,.22),vec2(.49,.30),rise);
            vec2 sealCenter=center+offset/aspect;
            // Original seals grow and turn upright, with alternating rotation directions.
            float progress=clamp((visionAge-(6.0+float(i)))/12.0,0.0,1.0);
            pattern=revealSymbol(pattern,sealCenter,aspect,sources[i],progress,visionOpacity,
                    (i%2==0?1.0:-1.0)*1.5707963);
        }
        pattern=revealDrawing(pattern,HomunculusRitualSampler,center,aspect,
                vec2(1199.0/1312.0,1.0)*.86,vec2(.5),vec2(1.0),
                clamp((visionAge-23.0)/17.0,0.0,1.0),phase,visionOpacity);
    }
    }
    return pattern;
}
void main() {
    vec3 scene=texture(InSampler,texCoord).rgb;
    float strength=Fractal.y, danger=Fractal.z;
    vec2 size=vec2(textureSize(InSampler,0));
    vec2 aspect=vec2(size.x/size.y,1.0);
    float pin=smoothstep(0.0,6.0,Eyes.w);
    vec2 anchor=mix(vec2(.5),Vision.xy,pin);
    // Let an off-screen apparition leave the view, rather than dragging the entire
    // background toward a perspective singularity or freezing its center at a clamp.
    vec2 outside=max(abs(anchor-vec2(.5))-vec2(.5),vec2(0))*aspect;
    float visibility=1.0-smoothstep(.15,.55,length(outside));
    float blend=clamp(Vision.w*visibility,0.0,1.0);
    vec3 normal=vec3(0), anchored=vec3(0);
    if(blend<1.0)normal=renderPattern(vec2(.5),0.0,0.0,0.0);
    if(blend>0.0)anchored=renderPattern(anchor,pin,Vision.z,1.0);
    // Crossfade layers, not their center coordinates. The eye never slides to the reticle.
    vec3 pattern=mix(clamp(normal,0.0,1.0),clamp(anchored,0.0,1.0),blend);
    float luminance=dot(scene,vec3(0.2126,0.7152,0.0722));
    // At full health there is no blanket overlay: only genuinely dark pixels reveal it.
    float shadow=1.0-smoothstep(0.015,0.20,luminance);
    float shadowOpacity=0.90*shadow*shadow*shadow;
    // Reserve broad screen coverage for low health, reaching full coverage at two hearts.
    float takeover=smoothstep(0.45,1.0,clamp(danger,0.0,1.0));
    takeover*=takeover;
    float opacity=strength*mix(shadowOpacity,1.0,takeover);
    fragColor=vec4(mix(scene,clamp(pattern,0.0,1.0),opacity),1.0);
}
