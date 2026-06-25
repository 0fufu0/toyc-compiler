    FUNC sum:
    PARAM n null null
    t0 = CONST(0)
    s = CONST(0)
    t1 = CONST(0)
    i = CONST(0)
    L0:
    t2 = i
    t3 = t2
    t4 = n
    t5 = t4
    t6 = (t3 LT t5)
    IFZ t6 GOTO L3
    t7 = s
    t8 = t7
    t9 = i
    t10 = t9
    t11 = (t8 ADD t10)
    s = t11
    t12 = s
    t13 = t12
    t14 = CONST(10)
    t15 = t14
    t16 = (t13 GT t15)
    IFZ t16 GOTO L1
    t17 = s
    t18 = t17
    t19 = CONST(1)
    t20 = t19
    t21 = (t18 SUB t20)
    s = t21
    GOTO L2
    L1:
    L2:
    t22 = i
    t23 = t22
    t24 = CONST(1)
    t25 = t24
    t26 = (t23 ADD t25)
    i = t26
    GOTO L0
    L3:
    t27 = s
    RET t27
    ENDFUNC
    FUNC main:
    t28 = CONST(5)
    a = CONST(5)
    t29 = a
    ARG t29 null null
    t30 = CALL sum
    b = t30
    t31 = b
    RET t31
    ENDFUNC
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
