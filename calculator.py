#!/usr/bin/env python3
"""Simple command-line calculator."""

import operator

OPERATIONS = {
    "+": operator.add,
    "-": operator.sub,
    "*": operator.mul,
    "/": operator.truediv,
}


def calculate(a: float, op: str, b: float) -> float:
    """Perform a calculation with two operands and an operator."""
    if op not in OPERATIONS:
        raise ValueError(f"Unknown operator: {op}. Use one of: {', '.join(OPERATIONS)}")
    if op == "/" and b == 0:
        raise ZeroDivisionError("Division by zero")
    return OPERATIONS[op](a, b)


def main():
    print("Calculator")
    print("Supported operations: +, -, *, /")
    print("Type 'q' to quit.\n")

    while True:
        expr = input("Enter expression (e.g. 2 + 3): ").strip()
        if expr.lower() == "q":
            break

        parts = expr.split()
        if len(parts) != 3:
            print("Error: enter expression as: <number> <operator> <number>")
            continue

        try:
            a = float(parts[0])
            op = parts[1]
            b = float(parts[2])
            result = calculate(a, op, b)
            print(f"= {result}\n")
        except ValueError as e:
            print(f"Error: {e}\n")
        except ZeroDivisionError as e:
            print(f"Error: {e}\n")


if __name__ == "__main__":
    main()
