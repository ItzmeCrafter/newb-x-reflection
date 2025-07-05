$input v_color0, v_color1, v_fog, v_refl, v_texcoord0, v_lightmapUV, v_extra

#include <bgfx_shader.sh>
#include <newb_legacy.sh>

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_SeasonsTexture, 1);
SAMPLER2D(s_LightMapTexture, 2);

#ifdef ALPHA_TEST
  #define GLOW_PIXEL(C) C.a>0.9875 && C.a<0.99
#else
  #define GLOW_PIXEL(C) C.a>0.9875 && C.a<0.995
#endif

void main() {
  vec4 diffuse;
  vec4 color;

#if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY)
  diffuse = vec4(1.0,1.0,1.0,1.0);
  color = vec4(1.0,1.0,1.0,1.0);
#else
  diffuse = texture2D(s_MatTexture, v_texcoord0);

#ifdef ALPHA_TEST
  if (diffuse.a < 0.6) {
    discard;
  }
#endif

#if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
  diffuse.rgb *= mix(vec3(1.0,1.0,1.0),
                     texture2D(s_SeasonsTexture, v_color1.xy).rgb * 2.0,
                     v_color1.z);
#endif
  color = v_color0;
#endif

  diffuse.rgb *= diffuse.rgb;

  vec3 light_tint = texture2D(s_LightMapTexture, v_lightmapUV).rgb;
  light_tint = mix(light_tint.bbb, light_tint*light_tint, 0.35 + 0.65*v_lightmapUV.y*v_lightmapUV.y*v_lightmapUV.y);

  color.rgb *= light_tint;

  if (GLOW_PIXEL(diffuse)) {
    color.rgb = max(color.rgb, NL_GLOW_TEX*(0.995-diffuse.a)/(0.995-0.9875));
  }

#ifdef TRANSPARENT
  if (v_extra.b > 0.9) {
    diffuse.rgb = vec3_splat(1.0 - NL_WATER_TEX_OPACITY*(1.0 - diffuse.b*1.8));
    diffuse.a = color.a;
  }
#else
  diffuse.a = 1.0;
#endif

  diffuse.rgb *= color.rgb;

  if (v_extra.b > 0.9) {
    diffuse.rgb += v_refl.rgb*v_refl.a;
  } else if (v_refl.a > 0.0 && v_extra.g < 0.0) {
    // wet effect - only on xz plane
    float dy = abs(dFdy(v_extra.g));
    if (dy < 0.0002) {
      float mask = v_refl.a*(clamp(v_extra.r*10.0,8.2,8.8)-7.8);
      diffuse.rgb += v_refl.rgb*mask;
    }
  }

  diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);

  diffuse.rgb = colorCorrection(diffuse.rgb);

  gl_FragColor = diffuse;
}
