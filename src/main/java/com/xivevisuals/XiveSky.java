package com.xivevisuals;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Random;

/** Процедурное космическое небо: переливающиеся волны, звёзды, чёрная дыра с кольцом. */
public final class XiveSky {
    private static final float R = 100f;
    private static final int N = 72;   // сегментов по долготе
    private static final int M = 28;   // сегментов по широте
    private static final float LAT_MIN = -0.15f;
    private static final float LAT_MAX = (float) (Math.PI / 2);

    private static final int V = (N + 1) * (M + 1);
    private static final float[] DX = new float[V], DY = new float[V], DZ = new float[V];
    private static final float[] ELEV = new float[V];
    private static final int[] COLORS = new int[V];

    private static final int STAR_COUNT = 450;
    private static final float[][] STARS = new float[STAR_COUNT][5]; // dx, dy, dz, size, phase

    static {
        for (int j = 0; j <= M; j++) {
            float lat = LAT_MIN + (LAT_MAX - LAT_MIN) * j / M;
            for (int i = 0; i <= N; i++) {
                float lon = (float) (Math.PI * 2 * i / N);
                int idx = j * (N + 1) + i;
                DX[idx] = (float) (Math.cos(lat) * Math.cos(lon));
                DY[idx] = (float) Math.sin(lat);
                DZ[idx] = (float) (Math.cos(lat) * Math.sin(lon));
                ELEV[idx] = Math.max(0f, DY[idx]);
            }
        }
        Random rnd = new Random(1337);
        for (float[] s : STARS) {
            Vector3f d;
            do {
                d = new Vector3f(rnd.nextFloat() * 2 - 1, rnd.nextFloat() * 1.1f - 0.1f, rnd.nextFloat() * 2 - 1);
            } while (d.lengthSquared() > 1f || d.lengthSquared() < 0.05f);
            d.normalize();
            s[0] = d.x; s[1] = d.y; s[2] = d.z;
            s[3] = 0.25f + rnd.nextFloat() * 0.6f;
            s[4] = rnd.nextFloat() * 6.28f;
        }
    }

    public static void render(WorldRenderContext ctx) {
        if (ctx.world() == null) return;
        XiveConfig.Palette pal = XiveConfig.palette();
        float t = (ctx.world().getTime() + ctx.tickCounter().getTickDelta(false)) / 20f;

        // Матрица взгляда: только вращение камеры (небо не двигается вместе с игроком)
        Matrix4f m = new Matrix4f().rotation(new Quaternionf(ctx.camera().getRotation()).conjugate());

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawDome(m, pal, t);

        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        drawStars(m, t);
        drawBlackHole(m, pal, t);

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    // ---------------------------------------------------------------- купол неба

    private static void drawDome(Matrix4f m, XiveConfig.Palette p, float t) {
        for (int i = 0; i < V; i++) COLORS[i] = skyColor(DX[i], DY[i], DZ[i], ELEV[i], t, p);

        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (int j = 0; j < M; j++) {
            for (int i = 0; i < N; i++) {
                int a = j * (N + 1) + i, bb = a + 1, c = a + (N + 1) + 1, d = a + (N + 1);
                vtx(b, m, a); vtx(b, m, bb); vtx(b, m, c); vtx(b, m, d);
            }
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void vtx(BufferBuilder b, Matrix4f m, int idx) {
        b.vertex(m, DX[idx] * R, DY[idx] * R, DZ[idx] * R).color(COLORS[idx]);
    }

    private static int skyColor(float x, float y, float z, float elev, float t, XiveConfig.Palette p) {
        float a = x * 3.2f, b = y * 3.2f, c = z * 3.2f;
        // domain warping -> эффект «растекающегося дыма»
        float wx = s(b * 2.1f + t * 0.11f) + s(c * 1.7f - t * 0.08f);
        float wy = s(c * 2.3f + t * 0.09f) + s(a * 1.9f + t * 0.07f);
        float wz = s(a * 2.0f - t * 0.10f) + s(b * 1.6f + t * 0.06f);
        float n = s((a + wx * 0.9f) * 1.6f + t * 0.12f) * s((b + wy * 0.9f) * 2.0f - t * 0.10f)
                + 0.6f * s((c + wz * 0.9f) * 1.7f + t * 0.08f);
        n = n * 0.45f + 0.5f;

        int base = lerp(p.horizon(), p.zenith(), smooth(0f, 0.7f, elev));
        float strength = 0.45f + 0.55f * (1f - smooth(0.2f, 1f, elev) * 0.6f);
        int col = lerp(base, p.c1(), smooth(0.35f, 0.70f, n) * strength);
        col = lerp(col, p.c2(), smooth(0.62f, 0.95f, n) * strength);
        return 0xFF000000 | col;
    }

    // ---------------------------------------------------------------- звёзды

    private static void drawStars(Matrix4f m, float t) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Vector3f d = new Vector3f(), u = new Vector3f(), v = new Vector3f();
        for (float[] s : STARS) {
            d.set(s[0], s[1], s[2]);
            basis(d, u, v);
            float tw = 0.45f + 0.55f * s(t * 1.3f + s[4]);
            int alpha = (int) (255 * Math.max(0.1f, tw));
            int col = (alpha << 24) | 0xCFEFFF;
            float sz = s[3];
            quad(b, m, d.x * R, d.y * R, d.z * R, u, v, sz, sz, col);
        }
        BufferRenderer.drawWithGlobalProgram(b.end());
    }

    private static void quad(BufferBuilder b, Matrix4f m, float cx, float cy, float cz,
                             Vector3f u, Vector3f v, float su, float sv, int col) {
        b.vertex(m, cx - u.x * su - v.x * sv, cy - u.y * su - v.y * sv, cz - u.z * su - v.z * sv).color(col);
        b.vertex(m, cx + u.x * su - v.x * sv, cy + u.y * su - v.y * sv, cz + u.z * su - v.z * sv).color(col);
        b.vertex(m, cx + u.x * su + v.x * sv, cy + u.y * su + v.y * sv, cz + u.z * su + v.z * sv).color(col);
        b.vertex(m, cx - u.x * su + v.x * sv, cy - u.y * su + v.y * sv, cz - u.z * su + v.z * sv).color(col);
    }

    // ---------------------------------------------------------------- чёрная дыра

    private static void drawBlackHole(Matrix4f m, XiveConfig.Palette p, float t) {
        float ang = t * 0.01f;
        Vector3f dir = new Vector3f(0.62f, 0.30f, -0.55f);
        dir.rotateY(ang).normalize();
        Vector3f u = new Vector3f(), v = new Vector3f();
        basis(dir, u, v);
        Vector3f c = new Vector3f(dir).mul(R * 0.98f);

        float pulse = 0.8f + 0.2f * s(t * 0.9f);
        int hot = lerp(p.c2(), 0xFFFFFF, 0.35f);

        // 1) сплюснутое аккреционное кольцо (аддитивно)
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        ring(b, m, c, u, v, 9f, 30f, 0.26f, 0.4f, argb((int) (255 * pulse), hot), argb(0, p.c2()));
        BufferRenderer.drawWithGlobalProgram(b.end());

        // 2) чёрный диск (обычное смешивание)
        RenderSystem.defaultBlendFunc();
        BufferBuilder f = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        f.vertex(m, c.x, c.y, c.z).color(0xFF000000);
        for (int i = 0; i <= 48; i++) {
            float a = (float) (Math.PI * 2 * i / 48);
            f.vertex(m, c.x + (u.x * (float) Math.cos(a) + v.x * (float) Math.sin(a)) * 8.5f,
                        c.y + (u.y * (float) Math.cos(a) + v.y * (float) Math.sin(a)) * 8.5f,
                        c.z + (u.z * (float) Math.cos(a) + v.z * (float) Math.sin(a)) * 8.5f).color(0xFF000000);
        }
        BufferRenderer.drawWithGlobalProgram(f.end());

        // 3) тонкое светящееся кольцо вокруг диска (аддитивно)
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        BufferBuilder l = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        ring(l, m, c, u, v, 8.5f, 12f, 1f, 0f, argb((int) (230 * pulse), p.c1()), argb(0, p.c1()));
        BufferRenderer.drawWithGlobalProgram(l.end());
    }

    /** Кольцо между радиусами rIn и rOut; squash — сплюснутость по оси v, tilt — поворот в плоскости. */
    private static void ring(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f u, Vector3f v,
                             float rIn, float rOut, float squash, float tilt, int colIn, int colOut) {
        final int seg = 64;
        float ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (Math.PI * 2 * i / seg), a1 = (float) (Math.PI * 2 * (i + 1) / seg);
            ringVtx(b, m, c, u, v, rIn, a0, squash, ct, st, colIn);
            ringVtx(b, m, c, u, v, rOut, a0, squash, ct, st, colOut);
            ringVtx(b, m, c, u, v, rOut, a1, squash, ct, st, colOut);
            ringVtx(b, m, c, u, v, rIn, a1, squash, ct, st, colIn);
        }
    }

    private static void ringVtx(BufferBuilder b, Matrix4f m, Vector3f c, Vector3f u, Vector3f v,
                                float r, float a, float squash, float ct, float st, int col) {
        float px = (float) Math.cos(a) * r;
        float py = (float) Math.sin(a) * r * squash;
        float x = px * ct - py * st;
        float y = px * st + py * ct;
        b.vertex(m, c.x + u.x * x + v.x * y, c.y + u.y * x + v.y * y, c.z + u.z * x + v.z * y).color(col);
    }

    // ---------------------------------------------------------------- утилиты

    private static void basis(Vector3f d, Vector3f u, Vector3f v) {
        Vector3f up = Math.abs(d.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        d.cross(up, u).normalize();
        d.cross(u, v).normalize();
    }

    private static float s(float x) { return (float) Math.sin(x); }

    private static float smooth(float e0, float e1, float x) {
        float k = Math.max(0f, Math.min(1f, (x - e0) / (e1 - e0)));
        return k * k * (3f - 2f * k);
    }

    private static int lerp(int c0, int c1, float k) {
        k = Math.max(0f, Math.min(1f, k));
        int r = (int) (((c0 >> 16) & 255) * (1 - k) + ((c1 >> 16) & 255) * k);
        int g = (int) (((c0 >> 8) & 255) * (1 - k) + ((c1 >> 8) & 255) * k);
        int bl = (int) ((c0 & 255) * (1 - k) + (c1 & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    private static int argb(int a, int rgb) { return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF); }

    private XiveSky() {}
}
