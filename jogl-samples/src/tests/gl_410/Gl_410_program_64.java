/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_410;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER_BIT;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER_BIT;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2GL3.GL_DOUBLE;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.DoubleBuffer;
import java.nio.ShortBuffer;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GBarbieri
 */
public class Gl_410_program_64 extends Test {

    public static void main(String[] args) {
        Gl_410_program_64 gl_410_program_64 = new Gl_410_program_64();
    }

    public Gl_410_program_64() {
        super("gl-410-program-64", Profile.CORE, 4, 1);
    }

    private final String SHADERS_SOURCE = "double";
    private final String SHADERS_ROOT = "src/data/gl_410";

    private int vertexCount = 4;
    private int positionSize = vertexCount * 3 * Double.BYTES;
    private double[] positionData = {
        -1.0f, -1.0f, +0.0f,
        +1.0f, -1.0f, +0.0f,
        +1.0f, +1.0f, +0.0f,
        -1.0f, +1.0f, +0.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        0, 2, 3};

    private enum Buffer {
        F64,
        ELEMENT,
        MAX
    }

    private enum Program {
        VERT,
        FRAG,
        MAX
    }

    private int[] pipelineName = {0}, programName = new int[Program.MAX.ordinal()],
            bufferName = new int[Buffer.MAX.ordinal()], vertexArrayName = {0};
    private int uniformMvp, uniformDiffuse;
    private float[] projection = new float[16], view = new float[16], model = new float[16];
    private double[] mvp = new double[16];

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initVertexBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        return validated;
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        try {

            if (validated) {

                String[] vertexSourceContent = new String[]{
                    new Scanner(new File(SHADERS_ROOT + "/" + SHADERS_SOURCE + ".vert")).useDelimiter("\\A").next()};
                programName[Program.VERT.ordinal()]
                        = gl4.glCreateShaderProgramv(GL_VERTEX_SHADER, 1, vertexSourceContent);
            }

            if (validated) {

                String[] fragmentSourceContent = new String[]{
                    new Scanner(new File(SHADERS_ROOT + "/" + SHADERS_SOURCE + ".frag")).useDelimiter("\\A").next()};
                programName[Program.FRAG.ordinal()]
                        = gl4.glCreateShaderProgramv(GL_FRAGMENT_SHADER, 1, fragmentSourceContent);
            }

            if (validated) {

                validated = validated && checkProgram(gl4, programName[Program.VERT.ordinal()]);
                validated = validated && checkProgram(gl4, programName[Program.FRAG.ordinal()]);
            }

            if (validated) {

                uniformMvp = gl4.glGetUniformLocation(programName[Program.VERT.ordinal()], "mvp");
                uniformDiffuse = gl4.glGetUniformLocation(programName[Program.FRAG.ordinal()], "diffuse");
            }

            if (validated) {

                gl4.glGenProgramPipelines(1, pipelineName, 0);
                gl4.glBindProgramPipeline(pipelineName[0]);
                gl4.glUseProgramStages(pipelineName[0], GL_VERTEX_SHADER_BIT, programName[Program.VERT.ordinal()]);
                gl4.glUseProgramStages(pipelineName[0], GL_FRAGMENT_SHADER_BIT, programName[Program.FRAG.ordinal()]);
            }

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Gl_410_program_64.class.getName()).log(Level.SEVERE, null, ex);
        }

        return validated && checkError(gl4, "initProgram");
    }

    private boolean initVertexBuffer(GL4 gl4) {

        gl4.glGenBuffers(Buffer.MAX.ordinal(), bufferName, 0);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.F64.ordinal()]);
        DoubleBuffer positionBuffer = GLBuffers.newDirectDoubleBuffer(positionData);
        gl4.glBufferData(GL_ARRAY_BUFFER, positionSize, positionBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        return checkError(gl4, "initArrayBuffer");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName, 0);

        gl4.glBindVertexArray(vertexArrayName[0]);
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.F64.ordinal()]);
            gl4.glVertexAttribLPointer(Semantic.Attr.POSITION, 3, GL_DOUBLE, 3 * Double.BYTES, 0);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
        }
        gl4.glBindVertexArray(0);

        return checkError(gl4, "initVertexArray");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        FloatUtil.makePerspective(projection, 0, true, (float) Math.PI * 0.25f,
                (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
        view = view();
        FloatUtil.makeIdentity(model);
        FloatUtil.multMatrix(projection, view);
        FloatUtil.multMatrix(projection, model);

        for (int i = 0; i < projection.length; i++) {
            mvp[i] = projection[i];
        }

        gl4.glProgramUniformMatrix4dv(programName[Program.VERT.ordinal()], uniformMvp, 1, false, mvp, 0);
        gl4.glProgramUniform4dv(programName[Program.FRAG.ordinal()], uniformDiffuse, 1,
                new double[]{1.0f, 0.5f, 0.0f, 1.0f}, 0);

        gl4.glViewportIndexedfv(0, new float[]{0, 0, windowSize.x, windowSize.y}, 0);
        gl4.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.0f, 0.0f, 0.0f}, 0);

        gl4.glBindProgramPipeline(pipelineName[0]);

        gl4.glBindVertexArray(vertexArrayName[0]);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        gl4.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteBuffers(Buffer.MAX.ordinal(), bufferName, 0);
        gl4.glDeleteVertexArrays(1, vertexArrayName, 0);
        gl4.glDeleteProgram(programName[Program.VERT.ordinal()]);
        gl4.glDeleteProgram(programName[Program.FRAG.ordinal()]);
        gl4.glBindProgramPipeline(0);
        gl4.glDeleteProgramPipelines(1, pipelineName, 0);

        return true;
    }
}