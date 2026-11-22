#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D InSampler;
uniform sampler2D HistorySampler;
layout(std140) uniform StewBlurConfig { vec4 Blur; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

vec3 sampleScene(vec2 uv, vec2 edge) {
    return texture(InSampler, clamp(uv, edge, vec2(1.0) - edge)).rgb;
}

void main() {
    vec2 px = 1.0 / vec2(textureSize(InSampler, 0));
    vec2 edge = 0.5 * px;
    float strength = clamp(Blur.w, 0.0, 1.0);
    vec3 original = sampleScene(texCoord, edge);

    // Soft focus is visible even when the player stands still during Poison.
    vec2 r = px * (2.0 + 5.0 * strength);
    vec3 axial = sampleScene(texCoord + vec2( r.x, 0.0), edge)
               + sampleScene(texCoord + vec2(-r.x, 0.0), edge)
               + sampleScene(texCoord + vec2(0.0,  r.y), edge)
               + sampleScene(texCoord + vec2(0.0, -r.y), edge);
    vec3 diagonal = sampleScene(texCoord + vec2( r.x,  r.y), edge)
                  + sampleScene(texCoord + vec2(-r.x,  r.y), edge)
                  + sampleScene(texCoord + vec2( r.x, -r.y), edge)
                  + sampleScene(texCoord + vec2(-r.x, -r.y), edge);
    vec3 soft = original * 0.28 + axial * 0.12 + diagonal * 0.06;
    vec3 color = mix(original, soft, 0.88 * strength);

    // Extra smear follows camera turning; history adds subtle motion trails.
    vec2 direction = Blur.yz * strength;
    vec3 streak = sampleScene(texCoord - 2.0 * direction, edge) * 0.12
                + sampleScene(texCoord - direction, edge) * 0.22
                + original * 0.32
                + sampleScene(texCoord + direction, edge) * 0.22
                + sampleScene(texCoord + 2.0 * direction, edge) * 0.12;
    float streakMix = clamp(length(direction) * 42.0, 0.0, 0.78);
    color = mix(color, streak, streakMix);
    color = mix(color, texture(HistorySampler, texCoord).rgb, clamp(Blur.x, 0.0, 0.82));
    fragColor = vec4(color, 1.0);
}
