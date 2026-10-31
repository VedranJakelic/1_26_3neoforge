#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
layout(std140) uniform WisdomConfig { vec4 Lens; };
layout(location=0) in vec2 texCoord;
layout(location=0) out vec4 fragColor;
void main() {
    vec2 p = texCoord - vec2(0.5);
    vec2 circular = p * vec2(Lens.z, 1.0);
    float r = length(circular);
    float edge = 1.0 - smoothstep(0.08, 0.64, r);
    float center = 1.0 - smoothstep(0.0, 0.30, r);
    float pulse = 0.96 + 0.04 * sin(Lens.y * 2.3);
    // Strong central magnification, easing back into the existing outer bulge.
    // The positive, outward-increasing sample scale avoids image inversion/folding.
    float bulge = 0.38 * edge + 0.40 * center;
    vec2 uv = vec2(0.5) + p * (1.0 - bulge * Lens.x * pulse);
    vec2 halfPixel = 0.5 / vec2(textureSize(InSampler, 0));
    fragColor = texture(InSampler, clamp(uv, halfPixel, vec2(1.0) - halfPixel));
}
