/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_500;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import glm.vec._2.i.Vec2i;
import glm.vec._4.Vec4;
import framework.BufferUtils;
import framework.Caps;
import framework.DrawElementsIndirectCommand;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;

/**
 *
 * @author GBarbieri
 */
public class Gl_500_multi_draw_indirect_arb extends Test {

    public static void main(String[] args) {
        Gl_500_multi_draw_indirect_arb gl_500_multi_draw_indirect_arb = new Gl_500_multi_draw_indirect_arb();
    }

    public Gl_500_multi_draw_indirect_arb() {
        super("gl-500-multi-draw-indirect-arb", Profile.CORE, 4, 5, new Vec2i(640, 480),
                new Vec2(-Math.PI * 0.2f, Math.PI * 0.2f));
    }

    private final String SHADERS_SOURCE = "multi-draw-indirect";
    private final String SHADERS_ROOT = "src/data/gl_500";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    private int elementCount = 15;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        0, 2, 3,
        0, 1, 2,
        0, 1, 2,
        0, 2, 3};

    private int vertexCount = 11;
    private int vertexSize = vertexCount * glf.Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f,
        //        
        -0.5f, -1.0f,/**/ 0.0f, 1.0f,
        +1.5f, -1.0f,/**/ 1.0f, 1.0f,
        +0.5f, +1.0f,/**/ 1.0f, 0.0f,
        //        
        -0.5f, -1.0f,/**/ 0.0f, 1.0f,
        +0.5f, -1.0f,/**/ 1.0f, 1.0f,
        +1.5f, +1.0f,/**/ 1.0f, 0.0f,
        -1.5f, +1.0f,/**/ 0.0f, 0.0f};

    private int indirectBufferCount = 3;

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int INDIRECT = 3;
        public static final int VERTEX_INDIRECTION = 4;
        public static final int MAX = 5;
    }

    private class Texture {

        public static final int A = 0;
        public static final int B = 1;
        public static final int C = 2;
        public static final int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX),
            drawOffset = GLBuffers.newDirectIntBuffer(indirectBufferCount),
            drawCount = GLBuffers.newDirectIntBuffer(indirectBufferCount),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1),
            pipelineName = GLBuffers.newDirectIntBuffer(1), uniformArrayStrideInt = GLBuffers.newDirectIntBuffer(1);
    private Vec4[] viewport = new Vec4[indirectBufferCount];
    private int programName;

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;
        validated = validated && gl4.isExtensionAvailable("GL_ARB_indirect_parameters");

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }

        Caps caps = new Caps(gl4, Profile.CORE);

        viewport[0] = new Vec4(windowSize.x / 3.0f * 0.0f, 0, windowSize.x / 3, windowSize.y);
        viewport[1] = new Vec4(windowSize.x / 3.0f * 1.0f, 0, windowSize.x / 3, windowSize.y);
        viewport[2] = new Vec4(windowSize.x / 3.0f * 2.0f, 0, windowSize.x / 3, windowSize.y);

        gl4.glEnable(GL_DEPTH_TEST);
        gl4.glProvokingVertex(GL_FIRST_VERTEX_CONVENTION);

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER,
                this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
        ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER,
                this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

        ShaderProgram shaderProgram = new ShaderProgram();
        shaderProgram.init(gl4);

        programName = shaderProgram.program();

        gl4.glProgramParameteri(programName, GL_PROGRAM_SEPARABLE, GL_TRUE);

        shaderProgram.add(vertShaderCode);
        shaderProgram.add(fragShaderCode);
        shaderProgram.link(gl4, System.out);

        int[] activeUniform = {0};
        gl4.glGetProgramiv(programName, GL_ACTIVE_UNIFORMS, activeUniform, 0);

        for (int i = 0; i < activeUniform[0]; ++i) {

            byte[] name = new byte[128];
            int[] length = {0};

            gl4.glGetActiveUniformName(programName, i, name.length, length, 0, name, 0);

            String stringName = new String(name).trim();

            if (stringName.equals("indirection.Transform[0]")) {
                IntBuffer uniformIndices = GLBuffers.newDirectIntBuffer(new int[]{i});
                gl4.glGetActiveUniformsiv(programName, 1, uniformIndices, GL_UNIFORM_ARRAY_STRIDE, uniformArrayStrideInt);
                BufferUtils.destroyDirectBuffer(uniformIndices);
            }
        }

        if (validated) {

            gl4.glGenProgramPipelines(1, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT, programName);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initBuffer(GL4 gl4) {

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(elementBuffer);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        int[] vertexIndirection = {0, 1, 2};
        int paddingInt = Math.max(Integer.BYTES, uniformArrayStrideInt.get(0));
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.VERTEX_INDIRECTION));
        gl4.glBufferData(GL_UNIFORM_BUFFER, paddingInt * 3, null, GL_DYNAMIC_DRAW);
        IntBuffer paddingIntBuffer = GLBuffers.newDirectIntBuffer(1);
        paddingIntBuffer.put(vertexIndirection[0]).rewind();
        gl4.glBufferSubData(GL_UNIFORM_BUFFER, paddingInt * 0, Integer.BYTES, paddingIntBuffer);
        paddingIntBuffer.put(vertexIndirection[1]).rewind();
        gl4.glBufferSubData(GL_UNIFORM_BUFFER, paddingInt * 1, Integer.BYTES, paddingIntBuffer);
        paddingIntBuffer.put(vertexIndirection[2]).rewind();
        gl4.glBufferSubData(GL_UNIFORM_BUFFER, paddingInt * 2, Integer.BYTES, paddingIntBuffer);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferData(GL_UNIFORM_BUFFER, Mat4.SIZE * indirectBufferCount, null, GL_DYNAMIC_DRAW);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        DrawElementsIndirectCommand[] commands = new DrawElementsIndirectCommand[6];
        commands[0] = new DrawElementsIndirectCommand(elementCount, 1, 0, 0, 0);
        commands[1] = new DrawElementsIndirectCommand(elementCount >> 1, 1, 6, 4, 1);
        commands[2] = new DrawElementsIndirectCommand(elementCount, 1, 9, 7, 2);
        commands[3] = new DrawElementsIndirectCommand(elementCount, 1, 0, 0, 0);
        commands[4] = new DrawElementsIndirectCommand(elementCount >> 1, 1, 6, 4, 1);
        commands[5] = new DrawElementsIndirectCommand(elementCount, 1, 9, 7, 2);

        drawCount.put(0, 3);
        drawCount.put(1, 2);
        drawCount.put(2, 1);
        drawOffset.put(0, 0);
        drawOffset.put(1, 1);
        drawOffset.put(2, 3);

        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferName.get(Buffer.INDIRECT));
        IntBuffer commandsBuffer = GLBuffers.newDirectIntBuffer(5 * commands.length);
        for (DrawElementsIndirectCommand command : commands) {
            commandsBuffer.put(command.toIa_());
        }
        /**
         * Critical, I forgot once to rewing the buffer, the driver video crashed.
         */
        commandsBuffer.rewind();
        gl4.glBufferData(GL_DRAW_INDIRECT_BUFFER, DrawElementsIndirectCommand.SIZE * commands.length, commandsBuffer,
                GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        BufferUtils.destroyDirectBuffer(commandsBuffer);

        return true;
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, glf.Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl4.glBindVertexArray(0);

        return true;
    }

    private boolean initTexture(GL4 gl4) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            assert (!texture.empty());
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl4.glGenTextures(Texture.MAX, textureName);
            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.A));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_RED);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, GLint(Texture.levels() - 1));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl4.glTexStorage2D(GL_TEXTURE_2D, texture.levels(), format.internal.value,
                    texture.dimensions()[0], texture.dimensions()[1]);
            for (int level = 0; level < texture.levels(); ++level) {

                gl4.glTexSubImage2D(GL_TEXTURE_2D, level,
                        0, 0,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.B));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, GLint(Texture.levels() - 1));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl4.glTexStorage2D(GL_TEXTURE_2D, texture.levels(), format.internal.value,
                    texture.dimensions()[0], texture.dimensions()[1]);
            for (int level = 0; level < texture.levels(); ++level) {

                gl4.glTexSubImage2D(GL_TEXTURE_2D, level,
                        0, 0,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.C));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_NONE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            //glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, GLint(Texture.levels() - 1));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl4.glTexStorage2D(GL_TEXTURE_2D, texture.levels(), format.internal.value,
                    texture.dimensions()[0], texture.dimensions()[1]);
            for (int level = 0; level < texture.levels(); ++level) {

                gl4.glTexSubImage2D(GL_TEXTURE_2D, level,
                        0, 0,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl4.glBindTexture(GL_TEXTURE_2D, 0);
            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_500_multi_draw_indirect_arb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private void validate(GL4 gl4) {

        int[] status = {0};
        gl4.glValidateProgramPipeline(pipelineName.get(0));
        gl4.glGetProgramPipelineiv(pipelineName.get(0), GL_VALIDATE_STATUS, status, 0);

        if (status[0] != GL_TRUE) {

            int[] lengthMax = {0};
            gl4.glGetProgramPipelineiv(pipelineName.get(0), GL_INFO_LOG_LENGTH, lengthMax, 0);

            IntBuffer lengthQuery = GLBuffers.newDirectIntBuffer(1);
            ByteBuffer infoLog = GLBuffers.newDirectByteBuffer(lengthMax[0] + 1);
            gl4.glGetProgramPipelineInfoLog(pipelineName.get(0), infoLog.capacity(), lengthQuery, infoLog);

            gl4.glDebugMessageInsert(
                    GL_DEBUG_SOURCE_APPLICATION,
                    GL_DEBUG_TYPE_OTHER, 76,
                    GL_DEBUG_SEVERITY_LOW,
                    lengthQuery.get(0), new String(infoLog.array()).trim());
        }
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        float[] depth = {1.0f};
        gl4.glClearBufferfv(GL_DEPTH, 0, depth, 0);
        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);

        {
            gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0, Mat4.SIZE * indirectBufferCount,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, windowSize.x / 3.0f / windowSize.y,
                    0.1f, 100.0f);
            Mat4 view = viewMat4();
            Mat4 model = new Mat4(1.0f);

            pointer.asFloatBuffer().put(projection.mul_(view).translate(0.0f, 0.0f, 0.5f).toFa_());
            pointer.position(Mat4.SIZE * 1);
            pointer.asFloatBuffer().put(projection.mul_(view).translate(0.0f, 0.0f, 0.0f).toFa_());
            pointer.position(Mat4.SIZE * 2);
            pointer.asFloatBuffer().put(projection.mul(view).translate(0.0f, 0.0f, -0.5f).toFa_());
            pointer.rewind();

            gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.A));
        gl4.glActiveTexture(GL_TEXTURE1);
        gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.B));
        gl4.glActiveTexture(GL_TEXTURE2);
        gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.C));

        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.INDIRECTION, bufferName.get(Buffer.VERTEX_INDIRECTION));

        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferName.get(Buffer.INDIRECT));

        validate(gl4);

        for (int i = 0; i < indirectBufferCount; ++i) {

            gl4.glViewportIndexedfv(0, viewport[i].toFA_(), 0);
            gl4.glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, null, drawCount.get(i), 
                    DrawElementsIndirectCommand.SIZE);
        }

        return true;
    }
}
