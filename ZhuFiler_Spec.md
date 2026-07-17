# ZhuFiler 五章十九条开发规范

## 第一章 KOTLIN架构规范

**第1条** 本规范适用于 /src/main/kotlin/ 目录  
**第2条** /src/main/kotlin/ 下仅限 /zhu/filer/ 目录，禁止出现其他目录或文件  
**第3条** /src/main/kotlin/zhu/filer/ 下仅限创建一级子目录  
**第4条** /src/main/kotlin/zhu/filer/ 下禁止二级及以上嵌套  
**第5条** /src/main/kotlin/zhu/filer/ 每个子目录及根目录下仅限 .kt 文件  

## 第二章 KT代码规范

**第1条** 本规范适用于 /src/main/kotlin/ 下所有 .kt 文件  
**第2条** 每个 .kt 文件总字符数不超过5000  
**第3条** 永恒不变内容用 .xml 静态创建，变化内容用 .kt 实现，禁止混用  

## 第三章 RES架构规范

**第1条** 本规范适用于 /src/main/res/ 目录  
**第2条** /src/main/res/ 下仅限 layout values xml drawable-v24 mipmap-anydpi-v26 values-en 目录，禁止其他  
**第3条** /src/main/res/ 下每个目录及根目录下仅限 .xml 文件  
**第4条** 每个 Activity 对应一个 XML 布局，禁止多对一或一对零  

## 第四章 XML代码规范

**第1条** 本规范适用于 /src/main/res/ 下所有 .xml 文件  
**第2条** /src/main/res/drawable-v24/ 下图标类仅限 outline_ 前缀的 Material Icons 官方矢量  
**第3条** /src/main/res/drawable-v24/ 下控件装饰类仅限 widget_*.xml，且使用 shape/selector/ripple/layer-list，禁止位图和 <path> 绘图  

## 第五章 注释规范

**第1条** 本规范适用于 .kt 文件、.xml 文件与 .gradle 文件  
**第2条** 注释用中文，不用标点符号，只用短语  
**第3条** 只能用 // 单行，禁止 /* */  
**第4条** .kt 每个类和方法上方一行注释  
**第5条** .xml 不写注释  
**第6条** .gradle 每个闭包上方一行注释  
**第7条** 除此之外任何地方都不允许出现注释  