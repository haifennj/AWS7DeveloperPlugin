# AWS开发者插件

作者：
**haifennj**

## 功能

>注意：该插件仅适用于AWS 7平台

### AWS应用快速管理

#### `apps`的资源添加至local.gradle文件中
 
* 将App添加到local.gradle文件中

#### `apps`的资源快速软连接

AWS资源使用新的管理方式后，App的资源代码使用独立的Git库管理，和release分开了，插件提供了两种菜单：
* `Link App` ：仅仅使用软连接的形式部署到release资源中，方便使用该App。如果已经软连接，则会显示Already Linked。
* 如果应用已经添加到gradle中，但是未进行刷新，也有所提示


