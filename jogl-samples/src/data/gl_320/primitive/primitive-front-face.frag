#version 150 core

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

const vec4 frontColor = vec4(1.0, 0.5, 0.0, 1.0);
const vec4 backColor = vec4(0.0, 0.5, 1.0, 1.0);

out vec4 color;

void main()
{
    color = gl_FrontFacing ? frontColor : backColor;
}

