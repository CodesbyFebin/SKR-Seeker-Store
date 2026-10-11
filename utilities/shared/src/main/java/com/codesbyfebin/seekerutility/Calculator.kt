package com.codesbyfebin.seekerutility

object Calculator {
 fun evaluate(input:String):Double {
  require(input.length<=256)
  var i=0
  fun skip(){while(i<input.length && input[i].isWhitespace())i++}
  lateinit var expression:()->Double
  fun factor():Double {
   skip();require(i<input.length){"Missing value"}
   if(input[i]=='+' || input[i]=='-'){val negative=input[i++]=='-';val value=factor();return if(negative)-value else value}
   if(input[i]=='('){i++;val value=expression();skip();require(i<input.length && input[i++]==')'){"Missing closing parenthesis"};return value}
   val start=i;while(i<input.length && (input[i].isDigit() || input[i]=='.'))i++
   require(i>start){"Invalid expression"};return input.substring(start,i).toDouble()
  }
  fun term():Double {var value=factor();while(true){skip();if(i==input.length || input[i] !in "*/")return value;val op=input[i++];val right=factor();if(op=='/')require(right!=0.0){"Division by zero"};value=if(op=='*')value*right else value/right}}
  expression={var value=term();var more=true;while(more){skip();if(i==input.length || input[i] !in "+-")more=false else {val op=input[i++];val right=term();value=if(op=='+')value+right else value-right}};value}
  val result=expression();skip();require(i==input.length && result.isFinite()){ "Invalid expression" };return result
 }
}
