package dev.vanguard.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vanguard.VanguardClient;
import dev.vanguard.module.Module;
import dev.vanguard.module.ModuleManager;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Draws translucent filled shapes in the 3D world, visible through walls. Modules override
 * Module.onWorldRender and call box/line/ring; this class batches them into one draw per frame.
 * Based on the Fabric docs "Rendering in the World" page for 1.21.11.
 */
public final class WorldDraw {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(VanguardClient.MOD_ID, "pipeline/filled_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build());

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static final WorldDraw INSTANCE = new WorldDraw();

    private BufferBuilder buffer;
    private MappableRingBuffer vertexBuffer;
    private Matrix4fc pose;

    /** Camera position this frame (world coordinates). */
    public Vec3 cam = Vec3.ZERO;
    /** Partial tick for smoothing entity positions. */
    public float partial;

    private WorldDraw() {}

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(INSTANCE::onRender);
    }

    private void onRender(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        PoseStack matrices = context.matrices();
        cam = context.worldState().cameraRenderState.pos;
        partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        pose = matrices.last().pose();
        try {
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled()) m.onWorldRender(this);
            }
        } catch (RuntimeException ignored) {
            // A broken overlay should never crash the frame
        }
        matrices.popPose();

        if (buffer != null) flush(mc);
    }

    // ---- shapes (all in world coordinates) ----

    private BufferBuilder buf() {
        if (buffer == null) {
            buffer = new BufferBuilder(ALLOCATOR, PIPELINE.getVertexFormatMode(), PIPELINE.getVertexFormat());
        }
        return buffer;
    }

    private void vertex(double x, double y, double z, int argb) {
        float a = ((argb >>> 24) & 255) / 255f;
        float r = ((argb >>> 16) & 255) / 255f;
        float g = ((argb >>> 8) & 255) / 255f;
        float b = (argb & 255) / 255f;
        buf().addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, a);
    }

    /** Filled box. argb includes alpha, for example 0x66FF3030. */
    public void box(AABB b, int argb) {
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        // front, back, left, right, top, bottom
        vertex(x1, y1, z2, argb); vertex(x2, y1, z2, argb); vertex(x2, y2, z2, argb); vertex(x1, y2, z2, argb);
        vertex(x2, y1, z1, argb); vertex(x1, y1, z1, argb); vertex(x1, y2, z1, argb); vertex(x2, y2, z1, argb);
        vertex(x1, y1, z1, argb); vertex(x1, y1, z2, argb); vertex(x1, y2, z2, argb); vertex(x1, y2, z1, argb);
        vertex(x2, y1, z2, argb); vertex(x2, y1, z1, argb); vertex(x2, y2, z1, argb); vertex(x2, y2, z2, argb);
        vertex(x1, y2, z2, argb); vertex(x2, y2, z2, argb); vertex(x2, y2, z1, argb); vertex(x1, y2, z1, argb);
        vertex(x1, y1, z1, argb); vertex(x2, y1, z1, argb); vertex(x2, y1, z2, argb); vertex(x1, y1, z2, argb);
    }

    /** One quad, drawn with both windings so it shows from either side. */
    public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int argb) {
        vertex(a.x, a.y, a.z, argb); vertex(b.x, b.y, b.z, argb); vertex(c.x, c.y, c.z, argb); vertex(d.x, d.y, d.z, argb);
        vertex(d.x, d.y, d.z, argb); vertex(c.x, c.y, c.z, argb); vertex(b.x, b.y, b.z, argb); vertex(a.x, a.y, a.z, argb);
    }

    /** A flat ribbon facing the camera, used as a line. */
    public void line(Vec3 from, Vec3 to, double width, int argb) {
        Vec3 dir = to.subtract(from);
        Vec3 toMid = from.add(to).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(toMid);
        if (side.lengthSqr() < 1.0e-8) return;
        side = side.normalize().scale(width / 2.0);
        quad(from.add(side), from.subtract(side), to.subtract(side), to.add(side), argb);
    }

    /** Flat horizontal ring on the plane y = center.y. */
    public void ring(Vec3 center, double inner, double outer, int segments, int argb) {
        for (int i = 0; i < segments; i++) {
            double t0 = Math.PI * 2.0 * i / segments;
            double t1 = Math.PI * 2.0 * (i + 1) / segments;
            Vec3 a = center.add(Math.cos(t0) * inner, 0, Math.sin(t0) * inner);
            Vec3 b = center.add(Math.cos(t0) * outer, 0, Math.sin(t0) * outer);
            Vec3 c = center.add(Math.cos(t1) * outer, 0, Math.sin(t1) * outer);
            Vec3 d = center.add(Math.cos(t1) * inner, 0, Math.sin(t1) * inner);
            quad(a, b, c, d, argb);
        }
    }

    /** Entity position smoothed between ticks. */
    public Vec3 pos(Entity e) {
        return new Vec3(Mth.lerp(partial, e.xOld, e.getX()), Mth.lerp(partial, e.yOld, e.getY()), Mth.lerp(partial, e.zOld, e.getZ()));
    }

    /** Entity hitbox moved to its smoothed position. */
    public AABB boundsOf(Entity e) {
        return e.getBoundingBox().move(pos(e).subtract(e.position()));
    }

    // ---- drawing (from the Fabric docs custom render pipeline example) ----

    private void flush(Minecraft client) {
        MeshData builtBuffer = buffer.buildOrThrow();
        buffer = null;
        MeshData.DrawState drawParameters = builtBuffer.drawState();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);
        draw(client, builtBuffer, drawParameters, vertices, format);
        vertexBuffer.rotate();
    }

    private GpuBuffer upload(MeshData.DrawState drawParameters, VertexFormat format, MeshData builtBuffer) {
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) vertexBuffer.close();
            vertexBuffer = new MappableRingBuffer(() -> VanguardClient.MOD_ID + " world draw",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(
            vertexBuffer.currentBuffer().slice(0, builtBuffer.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.vertexBuffer(), mappedView.data());
        }
        return vertexBuffer.currentBuffer();
    }

    private static void draw(Minecraft client, MeshData builtBuffer, MeshData.DrawState drawParameters,
                             GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (PIPELINE.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            builtBuffer.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = PIPELINE.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.indexBuffer());
            indexType = builtBuffer.drawState().indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(PIPELINE.getVertexFormatMode());
            indices = shapeIndexBuffer.getBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.type();
        }

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
            .createCommandEncoder()
            .createRenderPass(() -> VanguardClient.MOD_ID + " world draw pass",
                client.getMainRenderTarget().getColorTextureView(), OptionalInt.empty(),
                client.getMainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            renderPass.setPipeline(PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);
            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }
        builtBuffer.close();
    }
}
