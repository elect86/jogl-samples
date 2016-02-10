/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_330;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES3.*;
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
import java.nio.FloatBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_330_query_counter extends Test {

    public static void main(String[] args) {
        Gl_330_query_counter gl_330_query_counter = new Gl_330_query_counter();
    }

    public Gl_330_query_counter() {
        super("gl-330-query-counter", Profile.CORE, 3, 3);
    }

    private final String SHADERS_SOURCE = "query-counter";
    private final String SHADERS_ROOT = "src/data/gl_330";

    private int vertexCount = 6;
    private int positionSize = vertexCount * Vec2.SIZE;
    private float[] positionData = {
        -1.0f, -1.0f,
        +1.0f, -1.0f,
        +1.0f, +1.0f,
        +1.0f, +1.0f,
        -1.0f, +1.0f,
        -1.0f, -1.0f};

    private class Query {

        public static final int BEGIN = 0;
        public static final int END = 1;
        public static final int MAX = 2;
    }

    private int[] vertexArrayName = {0}, bufferName = {0}, queryName = new int[Query.MAX];
    private int programName, uniformMvp;

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

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

        return validated && checkError(gl3, "begin");
    }

    private boolean initQuery(GL3 gl3) {

        int[] queryBits = {0};
        gl3.glGetQueryiv(GL_TIMESTAMP, GL_QUERY_COUNTER_BITS, queryBits, 0);

        boolean validated = queryBits[0] >= 30;
        if (validated) {
            gl3.glGenQueries(queryName.length, queryName, 0);
        }

        return validated && checkError(gl3, "initQuery");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl3);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            programName = shaderProgram.program();

            shaderProgram.link(gl3, System.out);
        }

        // Get variables locations
        if (validated) {

            uniformMvp = gl3.glGetUniformLocation(programName, "mvp");
            gl3.glUseProgram(programName);
            gl3.glUniform4fv(gl3.glGetUniformLocation(programName, "diffuse"), 1, new float[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);
            gl3.glUseProgram(0);
        }

        return validated & checkError(gl3, "initProgram");
    }

    // Buffer update using glBufferSubData
    protected boolean initBuffer(GL3 gl3) {

        // Generate a buffer object
        gl3.glGenBuffers(1, bufferName, 0);

        // Bind the buffer for use
        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[0]);

        // Reserve buffer memory but and copy the values
        FloatBuffer positionBuffer = GLBuffers.newDirectFloatBuffer(positionData);
        gl3.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(positionBuffer);

        // Unbind the buffer
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        return checkError(gl3, "initBuffer");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName, 0);
        gl3.glBindVertexArray(vertexArrayName[0]);
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[0]);
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

        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, 4.0f / 3.0f, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        gl3.glQueryCounter(queryName[Query.BEGIN], GL_TIMESTAMP);

        gl3.glViewport(0, 0, windowSize.x, windowSize.y);

        gl3.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 0);

        gl3.glUseProgram(programName);
        gl3.glUniformMatrix4fv(uniformMvp, 1, false, mvp.toFa_(), 0);

        gl3.glBindVertexArray(vertexArrayName[0]);
        gl3.glDrawArrays(GL_TRIANGLES, 0, vertexCount);

        gl3.glQueryCounter(queryName[Query.END], GL_TIMESTAMP);

        int[] availableBegin = {GL_FALSE};
        gl3.glGetQueryObjectiv(queryName[Query.BEGIN], GL_QUERY_RESULT_AVAILABLE, availableBegin, 0);

        int[] availableEnd = {GL_FALSE};
        gl3.glGetQueryObjectiv(queryName[Query.END], GL_QUERY_RESULT_AVAILABLE, availableEnd, 0);

        // The OpenGL implementations will wait for the query if it's not available
        long[] timeBegin = {0}, timeEnd = {0};
        gl3.glGetQueryObjecti64v(queryName[Query.BEGIN], GL_QUERY_RESULT, timeBegin, 0);
        gl3.glGetQueryObjecti64v(queryName[Query.END], GL_QUERY_RESULT, timeEnd, 0);

        //glGetInteger64v(GL_TIMESTAMP, &TimeBegin);
        //glGetInteger64v(GL_TIMESTAMP, &TimeEnd);
        System.out.println(availableBegin[0] + ", " + availableEnd[0] + " / Time stamp: " 
                + (timeEnd[0] - timeBegin[0]) / 1_000_000f + " ms");

        return (timeEnd[0] - timeBegin[0]) > 0;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteBuffers(1, bufferName, 0);
        gl3.glDeleteProgram(programName);
        gl3.glDeleteVertexArrays(1, vertexArrayName, 0);

        return checkError(gl3, "end");
    }
}
