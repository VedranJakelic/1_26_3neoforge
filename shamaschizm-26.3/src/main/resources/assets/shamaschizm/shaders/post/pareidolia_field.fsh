#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform EyeStyle { vec4 Style; };
layout(std140) uniform EyeConfig { vec4 Eye; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec2 size = vec2(textureSize(InSampler, 0));
    vec2 aspect = vec2(Eye.z, 1.0);
    // Fixed lattice of *tested scene candidates*, not a visible procedural grid.
    // Global even coordinates prevent the winning center moving with each pixel.
    vec2 spacing = vec2(max(2.0, 2.0 * (192.0/108.0) / Eye.z), 2.0);
    vec2 grid = floor(texCoord * size / spacing) * spacing;
    vec2 center = texCoord;
    float best = 0.0, radius = 0.025, confidence = 0.0;
    for (int y = -8; y <= 8; ++y) for (int x = -8; x <= 8; ++x) {
        vec2 p = (grid + vec2(x,y)*spacing + 0.5) / size;
        if (any(lessThan(p, vec2(0.0))) || any(greaterThan(p, vec2(1.0)))) continue;
        vec3 d = texture(InSampler, p).rgb;
        float r = d.g * 0.12;
        float support = 1.0 - smoothstep(0.75, 1.85, length((texCoord-p)*aspect) / max(r,0.001));
        float c = smoothstep(0.22, 0.80, d.r);
        float rank = c * support;
        if (rank > best) { best=rank; center=p; radius=r; confidence=c; }
    }
    // Subpixel center fit avoids snapping between adjacent strong detections.
    vec2 sum = vec2(0.0); float weights = 0.0;
    for (int y=-1; y<=1; ++y) for (int x=-1; x<=1; ++x) {
        vec2 p = center + vec2(x,y)/size;
        vec3 d = texture(InSampler,p).rgb;
        float w = pow(d.r,8.0) * (1.0-smoothstep(0.004,0.014,abs(d.g*0.12-radius)));
        sum += p*w; weights += w;
    }
    if (weights > 0.0001) center = sum/weights;
    // Encode DISPLACEMENT, not a low-precision center/identity. Changing the
    // winner now changes a target flow which is filtered over several seconds.
    radius = max(radius, 0.001);
    vec2 p = (texCoord-center)*aspect/radius;
    float support = 1.0-smoothstep(0.90,1.85,length(p));
    float direct = 1.0-smoothstep(0.035,0.14,length((center-0.5)*aspect));
    float darkness = texture(InSampler,center).b;
    // Stronger everywhere, strongest around genuinely dark scene features.
    float gain = mix(0.65, 1.0, darkness);
    float amount = Eye.y * confidence * support * (1.0-0.25*direct) * gain;
    float lid = max(0.20, 0.48*sqrt(max(0.0,1.0-p.x*p.x)));
    // Reshape the whole surrounding patch, with a continuous skirt at the edge.
    vec2 q = p;
    q.y *= 1.0 + amount * (1.0/lid - 1.0) * 0.80;
    vec2 gaze = clamp((vec2(0.5)-center)*aspect,-0.13,0.13);
    vec2 iris = p-gaze;
    float ir = length(iris);
    q += iris * amount * (1.30*exp(-pow(ir/0.29,2.0))
                        -0.35*exp(-pow((ir-0.52)/0.18,2.0)));
    vec2 bend = q-p;
    float bendLimit = 0.42;
    bend *= bendLimit/(bendLimit+length(bend));
    vec2 flow = clamp(bend*radius/aspect,vec2(-0.12),vec2(0.12));
    // The companion pass carries slow iris/lid shading through the same
    // 16-bit temporal filter as the displacement, without painting new hues.
    if (Style.x > 0.5) {
        float lidHeight = 0.48 * pow(max(0.0,1.0-p.x*p.x),0.72);
        float aperture = (1.0-smoothstep(lidHeight-0.045,lidHeight+0.045,abs(p.y)))
                       * (1.0-smoothstep(0.94,1.04,abs(p.x)));
        float rim = exp(-pow((abs(p.y)-lidHeight)/0.055,2.0))
                  * (1.0-smoothstep(0.90,1.04,abs(p.x)));
        float irisDisk = 1.0-smoothstep(0.30,0.35,ir);
        float pupil = 1.0-smoothstep(0.105,0.145,ir);
        float irisRim = exp(-pow((ir-0.32)/0.035,2.0));
        float fibers = 0.5+0.5*sin(atan(iris.y,iris.x)*23.0+ir*19.0);
        float shade = rim*0.55 + aperture*(irisDisk*(0.16+0.10*fibers)
                    + pupil*0.75 + irisRim*0.40);
        // Small achromatic relief around the iris, never a white eyeball.
        float relief = aperture*(1.0-irisDisk)*0.35;
        float strength = clamp(amount,0.0,1.0);
        flow = vec2(clamp(shade*strength,0.0,0.85),relief*strength)*0.12;
    }
    // Two bytes per component: no multi-pixel RG8 center jumps at high resolution.
    vec2 encoded = floor(clamp(flow/0.24+0.5,0.0,1.0)*65535.0+0.5);
    fragColor = vec4(floor(encoded.x/256.0),mod(encoded.x,256.0),
                     floor(encoded.y/256.0),mod(encoded.y,256.0))/255.0;
}
