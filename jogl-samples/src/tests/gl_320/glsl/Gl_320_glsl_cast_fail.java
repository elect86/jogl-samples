/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_320.glsl;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2ES2.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Profile;
import framework.Semantic;
import framework.Test;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_glsl_cast_fail extends Test {

    public static void main(String[] args) {
        Gl_320_glsl_cast_fail gl_320_glsl_cast_fail = new Gl_320_glsl_cast_fail();
    }

    public Gl_320_glsl_cast_fail() {
        super("gl-320-glsl-cast-fail", Profile.CORE, 3, 2, Success.GENERATE_ERROR);
    }

    private final String SHADERS_SOURCE = "glsl-cast-fail";
    private final String SHADERS_ROOT = "src/data/gl_320/glsl";

    private int programName;

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl3);
        }

        return validated && checkError(gl3, "begin");
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

            gl3.glBindAttribLocation(programName, Semantic.Attr.POSITION, "Position");
            gl3.glBindAttribLocation(programName, Semantic.Attr.TEXCOORD, "Texcoord");
            gl3.glBindFragDataLocation(programName, Semantic.Frag.COLOR, "Color");

            shaderProgram.link(gl3, System.out);

            gl3.glDeleteProgram(programName);
            gl3.glDeleteShader(vertShaderCode.id());
            gl3.glDeleteShader(fragShaderCode.id());
        }

        return validated & checkError(gl3, "initProgram");
    }

    @Override
    protected boolean render(GL gl) {

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        return checkError(gl3, "end");
    }
}
