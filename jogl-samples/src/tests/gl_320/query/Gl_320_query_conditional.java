/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.query;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glm.vec._2.Vec2;
import glm.vec._4.Vec4;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_query_conditional extends Test {

    public static void main(String[] args) {
        Gl_320_query_conditional gl_320_query_conditional = new Gl_320_query_conditional();
    }

    public Gl_320_query_conditional() {
        super("gl-320-query-conditional", Profile.CORE, 3, 2);
    }

    private final String SHADERS_SOURCE = "query-conditional";
    private final String SHADERS_ROOT = "src/data/gl_320/query";

    private int vertexCount = 6;
    private int positionSize = vertexCount * Vec2.SIZE;
    private float[] positionData = {
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        +1.0f, +1.0f,
        +1.0f, +1.0f,
        -1.0f, +1.0f,
        -1.0f, -1.0f};

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int TRANSFORM = 1;
        public static final int MATERIAL = 2;
        public static final int MAX = 3;
    }

    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1), queryName = GLBuffers.newDirectIntBuffer(1),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private int programName, uniformMaterialOffset;
    private boolean toggle = true;
    private FloatBuffer clearBlackColor = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 1.0f}),
            clearWhiteColor = GLBuffers.newDirectFloatBuffer(new float[]{1.0f, 1.0f, 1.0f, 1.0f});

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        IntBuffer queryCounter = GLBuffers.newDirectIntBuffer(1);
        gl3.glGetQueryiv(GL_SAMPLES_PASSED, GL_QUERY_COUNTER_BITS, queryCounter);

        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }
        if (validated) {
            validated = initQuery(gl3);
        }

        BufferUtils.destroyDirectBuffer(queryCounter);

        return validated && checkError(gl3, "begin");
    }

    private boolean initQuery(GL3 gl3) {

        gl3.glGenQueries(1, queryName);

        IntBuffer queryBits = GLBuffers.newDirectIntBuffer(1);
        gl3.glGetQueryiv(GL_SAMPLES_PASSED, GL_QUERY_COUNTER_BITS, queryBits);

        boolean validated = queryBits.get(0) >= 32;

        BufferUtils.destroyDirectBuffer(queryBits);

        return validated && checkError(gl3, "initQuery");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE, "frag", null, true);

            ShaderProgram shaderProgram = new ShaderProgram();
            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.init(gl3);

            programName = shaderProgram.program();

            gl3.glBindAttribLocation(programName, Semantic.Attr.POSITION, "position");
            gl3.glBindFragDataLocation(programName, Semantic.Frag.COLOR, "color");

            shaderProgram.link(gl3, System.out);
        }
        if (validated) {

            gl3.glUniformBlockBinding(
                    programName,
                    gl3.glGetUniformBlockIndex(programName, "Transform"),
                    Semantic.Uniform.TRANSFORM0);
            gl3.glUniformBlockBinding(
                    programName,
                    gl3.glGetUniformBlockIndex(programName, "Material"),
                    Semantic.Uniform.MATERIAL);
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);
        FloatBuffer positionBuffer = GLBuffers.newDirectFloatBuffer(positionData);

        gl3.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);

        gl3.glGenBuffers(Buffer.MAX, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl3.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int uniformTransformBlockSize = Math.max(Mat4.SIZE, uniformBufferOffset.get(0));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformTransformBlockSize, null, GL_DYNAMIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        uniformMaterialOffset = Math.max(Vec4.SIZE, uniformBufferOffset.get(0));

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.MATERIAL));
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformMaterialOffset * 2, null, GL_STATIC_DRAW);

        {
            ByteBuffer pointer = gl3.glMapBufferRange(GL_UNIFORM_BUFFER, 0,
                    uniformMaterialOffset * 2, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);
            
            float[][] materials = {new float[]{0.0f, 0.5f, 1.0f, 1.0f}, new float[]{1.0f, 0.5f, 0.0f, 1.0f}};

            for (int i = 0; i < materials.length; i++) {
                for (int j = 0; j < materials[0].length; j++) {
                    pointer.putFloat(i * uniformMaterialOffset + j * Float.BYTES, materials[i][j]);
                }
            }
            pointer.rewind();

            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(uniformBufferOffset);
        BufferUtils.destroyDirectBuffer(positionBuffer);

        return checkError(gl3, "initBuffer");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0, 0);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
            ByteBuffer pointer = gl3.glMapBufferRange(GL_UNIFORM_BUFFER, 0, Mat4.SIZE,
                    GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
            Mat4 model = new Mat4(1.0f);

            pointer.asFloatBuffer().put(projection.mul(viewMat4()).mul(model).toFa_());

            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        // Set the display viewport
        gl3.glViewport(0, 0, windowSize.x, windowSize.y);
        gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        // Clear color buffer with black
        gl3.glClearBufferfv(GL_COLOR, 0, clearBlackColor);

        // Bind program
        gl3.glUseProgram(programName);
        gl3.glBindVertexArray(vertexArrayName.get(0));

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL), 0, Mat4.SIZE);

        // The first orange quad is not written in the framebuffer.
        gl3.glColorMaski(0, false, false, false, false);

        // Beginning of the samples count query
        gl3.glBeginQuery(GL_SAMPLES_PASSED, queryName.get(0));
        {
            // Added a boolean flag to trigger the conditional rendering without commenting anything
            if (toggle) {
                // To test the condional rendering, comment this line, the next draw call won't happen.
                gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
                // End of the samples count query
            }
        }
        gl3.glEndQuery(GL_SAMPLES_PASSED);

        // The second blue quad is written in the framebuffer only if a sample pass the occlusion query.
        gl3.glColorMaski(0, true, true, true, true);

        gl3.glBindBufferRange(GL_UNIFORM_BUFFER, Semantic.Uniform.MATERIAL, bufferName.get(Buffer.MATERIAL),
                uniformMaterialOffset, Mat4.SIZE);

        // Draw only if one sample went through the tests, 
        // we don't need to get the query result which prevent the rendering pipeline to stall.
        gl3.glBeginConditionalRender(queryName.get(0), GL_QUERY_WAIT);
        {
            // Clear color buffer with white
            gl3.glClearBufferfv(GL_COLOR, 0, clearWhiteColor);

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
        }
        gl3.glEndConditionalRender();

        toggle = !toggle;

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteProgram(programName);
        gl3.glDeleteBuffers(Buffer.MAX, bufferName);
        gl3.glDeleteVertexArrays(1, vertexArrayName);
        gl3.glDeleteQueries(1, queryName);

        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(queryName);

        BufferUtils.destroyDirectBuffer(clearWhiteColor);
        BufferUtils.destroyDirectBuffer(clearBlackColor);

        return checkError(gl3, "end");
    }
}
