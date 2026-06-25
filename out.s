.text
.globl _start
_start:
call main
li a7, 10
ecall
.globl sum
sum:
sw s0, 0(sp)
mv s0, sp
addi sp, sp, -128
li t0, 0
sw t0, -4(sp)
li t0, 0
sw t0, -8(sp)
li t0, 0
sw t0, -12(sp)
li t0, 0
sw t0, -16(sp)
L0:
lw t0, -16(sp)
sw t0, -20(sp)
lw t0, -20(sp)
sw t0, -24(sp)
lw t0, 4(s0)
sw t0, -28(sp)
lw t0, -28(sp)
sw t0, -32(sp)
lw t0, -24(sp)
lw t1, -32(sp)
slt t2, t0, t1
sw t2, -36(sp)
lw t0, -36(sp)
beqz t0, L3
lw t0, -8(sp)
sw t0, -40(sp)
lw t0, -40(sp)
sw t0, -44(sp)
lw t0, -16(sp)
sw t0, -48(sp)
lw t0, -48(sp)
sw t0, -52(sp)
lw t0, -44(sp)
lw t1, -52(sp)
add t2, t0, t1
sw t2, -56(sp)
lw t0, -56(sp)
sw t0, -8(sp)
lw t0, -8(sp)
sw t0, -60(sp)
lw t0, -60(sp)
sw t0, -64(sp)
li t0, 10
sw t0, -68(sp)
lw t0, -68(sp)
sw t0, -72(sp)
lw t0, -64(sp)
lw t1, -72(sp)
slt t2, t1, t0
sw t2, -76(sp)
lw t0, -76(sp)
beqz t0, L1
lw t0, -8(sp)
sw t0, -80(sp)
lw t0, -80(sp)
sw t0, -84(sp)
li t0, 1
sw t0, -88(sp)
lw t0, -88(sp)
sw t0, -92(sp)
lw t0, -84(sp)
lw t1, -92(sp)
sub t2, t0, t1
sw t2, -96(sp)
lw t0, -96(sp)
sw t0, -8(sp)
j L2
L1:
L2:
lw t0, -16(sp)
sw t0, -100(sp)
lw t0, -100(sp)
sw t0, -104(sp)
li t0, 1
sw t0, -108(sp)
lw t0, -108(sp)
sw t0, -112(sp)
lw t0, -104(sp)
lw t1, -112(sp)
add t2, t0, t1
sw t2, -116(sp)
lw t0, -116(sp)
sw t0, -16(sp)
j L0
L3:
lw t0, -8(sp)
sw t0, -120(sp)
lw a0, -120(sp)
addi sp, sp, 128
lw s0, 0(sp)
ret
.globl main
main:
sw s0, 0(sp)
mv s0, sp
addi sp, sp, -32
li t0, 5
sw t0, -4(sp)
li t0, 5
sw t0, -8(sp)
lw t0, -8(sp)
sw t0, -12(sp)
lw t0, -12(sp)
sw t0, 4(sp)
sw ra, -28(sp)
call sum
lw ra, -28(sp)
sw a0, -16(sp)
lw t0, -16(sp)
sw t0, -20(sp)
lw t0, -20(sp)
sw t0, -24(sp)
lw a0, -24(sp)
addi sp, sp, 32
lw s0, 0(sp)
ret
