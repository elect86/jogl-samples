/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_400;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fc4f;
import glm.vec._2.Vec2;
import java.nio.FloatBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_400_primitive_tessellation extends Test {

    public static void main(String[] args) {
        Gl_400_primitive_tessellation gl_400_primitive_tessellation = new Gl_400_primitive_tessellation();
    }

    public Gl_400_primitive_tessellation() {
        super("gl-400-primitive-tesselation", Profile.CORE, 4, 0);
    }

    private final String SHADERS_SOURCE = "tess";
    private final String SHADERS_ROOT = "src/data/gl_400";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * Vertex_v2fc4f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 1.0f, 0.0f, 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f, 0.0f, 1.0f,
        +1.0f, +1.0f,/**/ 0.0f, 1.0f, 0.0f, 1.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f, 1.0f, 1.0f};

    private int programName, uniformMvp;
    private int[] arrayBufferName = {0}, vertexArrayName = {0};

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        return validated && checkError(gl4, "begin");
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "vert", null, true);
            ShaderCode contShaderCode = ShaderCode.create(gl4, GL_TESS_CONTROL_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "cont", null, true);
            ShaderCode evalShaderCode = ShaderCode.create(gl4, GL_TESS_EVALUATION_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "eval", null, true);
            ShaderCode geomShaderCode = ShaderCode.create(gl4, GL_GEOMETRY_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "geom", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADERS_SOURCE, "frag", null, true);

            shaderProgram.init(gl4);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(geomShaderCode);
            shaderProgram.add(contShaderCode);
            shaderProgram.add(evalShaderCode);
            shaderProgram.add(fragShaderCode);

            programName = shaderProgram.program();

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            uniformMvp = gl4.glGetUniformLocation(programName, "mvp");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initVertexArray(GL4 gl4) {

        // Build a vertex array object
        gl4.glGenVertexArrays(1, vertexArrayName, 0);
        gl4.glBindVertexArray(vertexArrayName[0]);
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, arrayBufferName[0]);
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, Vertex_v2fc4f.SIZE, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_FLOAT, false, Vertex_v2fc4f.SIZE, Vec2.SIZE);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.COLOR);
        }
        gl4.glBindVertexArray(0);

        return checkError(gl4, "initVertexArray");
    }

    private boolean initBuffer(GL4 gl4) {

        gl4.glGenBuffers(1, arrayBufferName, 0);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, arrayBufferName[0]);
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        return checkError(gl4, "initBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        gl4.glViewport(0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.0f, 0.0f, 0.0f}, 0);

        gl4.glUseProgram(programName);
        gl4.glUniformMatrix4fv(uniformMvp, 1, false, mvp.toFa_(), 0);

        gl4.glBindVertexArray(vertexArrayName[0]);
        gl4.glPatchParameteri(GL_PATCH_VERTICES, vertexCount);
        gl4.glPatchParameterfv(GL_PATCH_DEFAULT_INNER_LEVEL, new float[]{16.f, 16.f}, 0);
        gl4.glPatchParameterfv(GL_PATCH_DEFAULT_OUTER_LEVEL, new float[]{16.f, 16.f, 16.f, 16.f}, 0);
        gl4.glDrawArraysInstanced(GL_PATCHES, 0, vertexCount, 1);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteVertexArrays(1, vertexArrayName, 0);
        gl4.glDeleteBuffers(1, arrayBufferName, 0);
        gl4.glDeleteProgram(programName);

        return checkError(gl4, "end");
    }
}
