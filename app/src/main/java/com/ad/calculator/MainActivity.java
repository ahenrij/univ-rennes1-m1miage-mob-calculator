package com.ad.calculator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    public static final String CURR_OP_KEY = "currentOperatorKey";
    public static final String CURR_DISPLAY_KEY = "currentDisplayKey";

    private int currentOperatorId;
    private String currentOperator;
    private String currentDisplay;
    private EditText etDisplay;
    private ImageView imgBackspace;

    //If there was a calculation and a number is typed
    private boolean shouldStartOver = false;


    private String OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD;

    private String[] operators;

    Double ok = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        changeStatusBarColor();

        etDisplay = findViewById(R.id.et_display);
        imgBackspace = findViewById(R.id.img_backspace);

        initValues(savedInstanceState);
    }

    private void initValues(Bundle savedInstanceState) {

        OP_ADD = getResources().getString(R.string.op_add);
        OP_SUB = getResources().getString(R.string.op_sub);
        OP_MUL = getResources().getString(R.string.op_mul);
        OP_DIV = getResources().getString(R.string.op_div);
        OP_MOD = getResources().getString(R.string.op_mod);

        operators = new String[]{OP_ADD, OP_SUB, OP_MUL, OP_DIV, OP_MOD};

        if (savedInstanceState != null) {
            setCurrentDisplay(savedInstanceState.getString(CURR_DISPLAY_KEY, ""));
            setCurrentOperator(savedInstanceState.getString(CURR_OP_KEY, ""));
        } else {
            setCurrentDisplay("");
            setCurrentOperator("");
        }

        imgBackspace.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                clearAll(view);
                return true;
            }
        });
    }

    public void addOperator(View view) {

        if (!currentDisplay.isEmpty()) {

            String operator = ((Button) view).getText().toString();
            Integer operatorId = view.getId();

            //For unary minus verification, make sure operator is the current one
            if (!currentOperator.isEmpty()) {

                //Operator at the display's end
                if (displayEndsWithOperator()) {
                    //Replace the operator
                    currentDisplay = currentDisplay.substring(0, currentDisplay.length()-1);
                    addOperatorToDisplay(operator, operatorId);

                } else {

                    //There is an operator which is not at the end of expr, make the evaluation...
                    if (evaluateDisplay()) { //...and append the new operator if evaluation succeed
                        addOperatorToDisplay(operator, operatorId);
                    }
                }

            } else {
                addOperatorToDisplay(operator, operatorId);
            }
        }
        if (shouldStartOver) {
            shouldStartOver = false;
        }
    }

    /**
     * @param view  Button holding the single number to append as text
     * @post Append number or . to display if allowed
     */
    public void addNumber(View view) {
        String number = ((Button) view).getText().toString();

        if (number.equals(".")) {

            if (!getCurrentOperand().contains(".")) {
                if (displayEndsWithOperator() || currentDisplay.isEmpty()) {
                    number = "0" + number;
                }
                setCurrentDisplay(currentDisplay + number);
            }

        } else {

            if (shouldStartOver) {
                setCurrentDisplay(number);
                shouldStartOver = false;
            } else {
                setCurrentDisplay((currentDisplay.equals("0")) ? number : currentDisplay + number);
            }
        }

    }

    public void doBackSpace(View view) {
        if (!currentDisplay.isEmpty()) {
            setCurrentDisplay(currentDisplay.substring(0, currentDisplay.length() - 1));
        }
    }

    public void clearAll(View view) {
        setCurrentDisplay("");
    }

    public void doCalculation(View view) {

        if (displayEndsWithOperator()) {
            Toast.makeText(this, "Un format non valide est utilisé.", Toast.LENGTH_SHORT).show();
        } else if (displayContainsOperator()) {
            evaluateDisplay();
        }
    }

    private void setCurrentDisplay(String currentDisplay) {
        this.currentDisplay = currentDisplay;
        etDisplay.setText(currentDisplay);
    }

    private void setCurrentOperator(String currentOperator) {
        this.currentOperator = currentOperator;
    }

    private boolean displayContainsOperator() {
        return !this.currentOperator.isEmpty();
    }

    private boolean displayEndsWithOperator() {

        boolean endsWith = false;
        for (String operator: operators) {
            if (currentDisplay.endsWith(operator)) {
                endsWith = true;
                break;
            }
        }
        return endsWith;
    }


    private void addOperatorToDisplay(String operator, Integer id) {
        if (!displayContainsOperator()) {
            setCurrentDisplay(currentDisplay + operator);
            setCurrentOperator(operator);
            this.currentOperatorId = id;
        }
    }

    /**
     * @pre display contains an operator not at the end : an operation !!
     * @post calculate expression display and set it to display
     * @return true if display has been properly evaluated, and false + toast error message if not
     */
    private boolean evaluateDisplay() {

        double result = 0;
        //Quote operator to avoid special characters regex exception
        String[] operands = currentDisplay.split(Pattern.quote(this.currentOperator));

        switch (this.currentOperator) {
            case "+":
                result = Double.parseDouble(operands[0]) + Double.parseDouble(operands[1]);
                break;
            case "-":
                result = Double.parseDouble(operands[0]) - Double.parseDouble(operands[1]);
                break;
            case "÷":
                if (Double.parseDouble(operands[1]) == 0.0) {
                    Toast.makeText(this, "Division par zéro invalide", Toast.LENGTH_SHORT).show();
                    return false;
                }
                result = Double.parseDouble(operands[0]) / Double.parseDouble(operands[1]);
                break;
            case "×":
                result = Double.parseDouble(operands[0]) * Double.parseDouble(operands[1]);
                break;
            case "%":
                result = mod(Integer.parseInt(operands[0].split(Pattern.quote("."))[0]), Integer.parseInt(operands[1].split(Pattern.quote("."))[0]));
                break;
            default:
                Toast.makeText(this, "Operateur non reconnu.", Toast.LENGTH_SHORT).show();
                return false;
        }

        //if result if integer, show it as int
        if (doubleIsInteger(result)) {
            setCurrentDisplay(String.valueOf((int) result));
        } else {
            //Show double result with precision of result_precision integer after . if needed
            setCurrentDisplay((String.valueOf(result).split(Pattern.quote("."))[1].length() > getResources().getInteger(R.integer.result_precision)) ? String.format(Locale.FRANCE,"%." +  getResources().getInteger(R.integer.result_precision) + "f", result).replace(",", ".") : String.valueOf(result));
        }
        shouldStartOver = true;
        currentOperator = "";
        return true;
    }

    /**
     *
     * @return the current operand that user is typing and empty if no operands
     */
    private String getCurrentOperand() {
        if (!displayContainsOperator()) {
            return currentDisplay;
        } else {
            return displayEndsWithOperator() ? "" : currentDisplay.split(Pattern.quote(this.currentOperator))[1];
        }
    }

    /**
     * @return x modulo y
     */
    private int mod(int x, int y) {
        int result = x % y;
        return result < 0? result + y : result;
    }

    /**
     *
     * @param result (of operation)
     * @return true if result is an integer (.0)
     */
    private boolean doubleIsInteger(Double result) {
        return (result == Math.floor(result)) && !Double.isInfinite(result);
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#bdbdbd"));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(CURR_OP_KEY, this.currentOperator);
        outState.putString(CURR_DISPLAY_KEY, this.currentDisplay);
    }
}