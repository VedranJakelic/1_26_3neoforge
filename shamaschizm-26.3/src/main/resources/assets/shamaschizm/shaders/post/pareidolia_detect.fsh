#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
// time, trip * depth strength, physical screen aspect, elapsed render seconds
layout(std140) uniform EyeConfig { vec4 Eye; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 aspect = vec2(Eye.z, 1.0);
    float best = 0.0, radius = 0.025, darkness = 0.0;
    // Broad enough for pixelated/partly broken outlines, but require agreement
    // around the whole circumference. A single straight edge must not qualify.
    for (int scale = 0; scale < 10; ++scale) {
        float r = 0.022 * pow(1.20, float(scale));
        if (any(lessThan(texCoord - vec2(r * 1.20) / aspect, vec2(0.0))) ||
            any(greaterThan(texCoord + vec2(r * 1.20) / aspect, vec2(1.0)))) continue;
        vec3 sum = vec3(0.0);
        float energy = 0.0, support = 0.0, square = 0.0, light = 0.0;
        for (int ray = 0; ray < 16; ++ray) {
            float a = 6.2831853 * (float(ray) + 0.5) / 16.0;
            vec2 d = vec2(cos(a), sin(a)) * r / aspect;
            vec3 outside = texture(InSampler, texCoord + d * 1.10).rgb;
            vec3 inside = texture(InSampler, texCoord + d * 0.90).rgb;
            vec3 boundary = texture(InSampler, texCoord + d).rgb;
            vec3 edge = outside - inside;
            vec3 ring = boundary - 0.5*(inside+outside);
            if (dot(ring,ring) > dot(edge,edge)) edge = ring;
            float luminance = dot((inside + outside + boundary)/3.0, vec3(0.2126,0.7152,0.0722));
            light += luminance;
            // Recover faint contrast in shadows without inventing edges in black.
            edge *= mix(8.0, 1.0, smoothstep(0.035, 0.38, luminance));
            float e = length(edge);
            sum += edge; energy += e; square += e * e;
            support += smoothstep(0.025, 0.10, e);
        }
        float mean = energy / 16.0;
        float coherence = length(sum) / max(energy, 0.0001);
        float uniformity = mean * mean / max(square / 16.0, 0.0001);
        float roundness = smoothstep(0.60, 0.94, support / 16.0)
                        * smoothstep(0.50, 0.90, coherence)
                        * smoothstep(0.48, 0.90, uniformity);
        float score = roundness * smoothstep(0.035, 0.24, mean);
        if (score > best) { best = score; radius = r; darkness = 1.0-smoothstep(0.035,0.38,light/16.0); }
    }
    fragColor = vec4(best, radius / 0.12, darkness, 1.0);
}
