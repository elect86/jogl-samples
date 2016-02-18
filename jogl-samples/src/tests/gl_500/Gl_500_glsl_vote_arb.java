/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_500;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import framework.DrawElementsIndirectCommand;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fv2f;
import glm.glm;
import glm.mat._4.Mat4;
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
public class Gl_500_glsl_vote_arb extends Test {

    public static void main(String[] args) {
        Gl_500_glsl_vote_arb gl_500_glsl_vote_arb = new Gl_500_glsl_vote_arb();
    }

    public Gl_500_glsl_vote_arb() {
        super("gl-500-glsl-vote-arb", Profile.CORE, 4, 5);
    }

    private final String SHADERS_SOURCE = "glsl-vote";
    private final String SHADERS_ROOT = "src/data/gl_500";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Buffer {

        public static final int TRANSFORM = 0;
        public static final int ELEMENT = 1;
        public static final int VERTEX = 2;
        public static final int INDIRECT = 3;
        public static final int MAX = 4;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX), pipelineName = GLBuffers.newDirectIntBuffer(1),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1), textureName = GLBuffers.newDirectIntBuffer(1);
    private int programName;
    private ByteBuffer uniformPointer;
    private FloatBuffer black = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 1.0f});

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        validated = validated && checkExtension(gl4, "GL_ARB_shader_group_vote");

        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }

        return validated;
    }

    private boolean initBuffer(GL4 gl4) {

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, 0);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_SHADER_STORAGE_BUFFER, bufferName.get(Buffer.VERTEX));
        gl4.glBufferStorage(GL_SHADER_STORAGE_BUFFER, vertexSize, vertexBuffer, 0);
        gl4.glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);
        int uniformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset.get(0));

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferStorage(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT
                | GL_MAP_COHERENT_BIT);
        uniformPointer = gl4.glMapBufferRange(GL_UNIFORM_BUFFER, 0, Mat4.SIZE, GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT
                | GL_MAP_COHERENT_BIT);

        DrawElementsIndirectCommand command = new DrawElementsIndirectCommand(elementCount, 1, 0, 0, 0);
        IntBuffer commandBuffer = GLBuffers.newDirectIntBuffer(command.toIa_());
        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferName.get(Buffer.INDIRECT));
        gl4.glBufferStorage(GL_DRAW_INDIRECT_BUFFER, DrawElementsIndirectCommand.SIZEOF, commandBuffer, 0);
        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(uniformBufferOffset);
        BufferUtils.destroyDirectBuffer(commandBuffer);

        return true;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        if (validated) {

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
        }

        if (validated) {

            gl4.glGenProgramPipelines(1, pipelineName);
            gl4.glUseProgramStages(pipelineName.get(0), GL_VERTEX_SHADER_BIT | GL_FRAGMENT_SHADER_BIT, programName);
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initTexture(GL4 gl4) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            assert (!texture.empty());
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl4.glGenTextures(1, textureName);
            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(0));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_RED);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_GREEN);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_BLUE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ALPHA);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            gl4.glTexStorage2D(GL_TEXTURE_2D, texture.levels(), format.internal.value, texture.dimensions(0)[0],
                    texture.dimensions(0)[1]);
            for (int level = 0; level < texture.levels(); ++level) {
                gl4.glTexSubImage2D(GL_TEXTURE_2D, level,
                        0, 0,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        format.external.value, format.type.value,
                        texture.data(level));
            }

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_500_glsl_vote_arb.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl4.glBindVertexArray(0);

        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        {
            Mat4 projection = glm.perspectiveFov_((float) Math.PI * 0.25f, (float) windowSize.x, windowSize.y, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);

            uniformPointer.asFloatBuffer().put(projection.mul(viewMat4()).mul(model).toFa_());
        }

        gl4.glViewportIndexedf(0, 0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, black);

        gl4.glBindProgramPipeline(pipelineName.get(0));
        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));
        gl4.glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, bufferName.get(Buffer.VERTEX));

        gl4.glBindTextures(Semantic.Sampler.DIFFUSE, 1, textureName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBuffer(GL_DRAW_INDIRECT_BUFFER, bufferName.get(Buffer.INDIRECT));

        gl4.glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_SHORT, null, 1, DrawElementsIndirectCommand.SIZEOF);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        BufferUtils.destroyDirectBuffer(bufferName);
        gl4.glDeleteProgram(programName);
        gl4.glDeleteProgramPipelines(1, pipelineName);
        BufferUtils.destroyDirectBuffer(pipelineName);
        gl4.glDeleteVertexArrays(1, vertexArrayName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return true;
    }
}
