.data
t0:
.word 10
g:
.word 10
.text
.globl _start
_start:
call main
li a7, 10
ecall
main:
sw s0, 0(sp)
mv s0, sp
addi sp, sp, -48
li t0, 3
la t6, t0
sw t0, 0(t6)
li t0, 3
sw t0, -4(sp)
li t0, 5
la t6, t0
sw t0, 0(t6)
li t0, 5
sw t0, -8(sp)
lw t0, -4(sp)
la t6, t0
sw t0, 0(t6)
la t6, t0
lw t0, 0(t6)
sw t0, -12(sp)
lw t0, -8(sp)
la t6, t0
sw t0, 0(t6)
la t6, t0
lw t0, 0(t6)
sw t0, -16(sp)
lw t0, -12(sp)
lw t1, -16(sp)
add t2, t0, t1
sw t2, -20(sp)
lw t0, -20(sp)
sw t0, -24(sp)
la t6, g
lw t0, 0(t6)
la t6, t0
sw t0, 0(t6)
la t6, t0
lw t0, 0(t6)
sw t0, -28(sp)
lw t0, -24(sp)
lw t1, -28(sp)
add t2, t0, t1
sw t2, -32(sp)
lw t0, -32(sp)
sw t0, -36(sp)
lw t0, -36(sp)
la t6, t0
sw t0, 0(t6)
la t6, t0
lw a0, 0(t6)
addi sp, sp, 48
lw s0, 0(sp)
ret
