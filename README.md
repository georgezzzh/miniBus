# miniBus
**武汉公交实时查询 | 智能公交无广告**
>前言:
>* 官方客户端广告是在太多了，而且整合了一大堆不需要的功能，简单写个精简版。如果有什么bug与异常，欢迎提issue。  
>* 首次启动应用后，授予权限之后首页为空白，下拉刷新一次即可。
>* 特殊公交信息可能匹配不到(数据源用的百度地图API)，可能有出入，xxx路(例如373路)公交匹配正常。
>* 本项目仅为学习与交流，如有侵权，立即删除。  
### 使用说明
* 点击[项目release](https://github.com/georgezzzh/miniBus/releases/download/1.0.0/minibus-1.0.0.apk), 找到apk文件下载安装即可。
* APP需要位置信息匹配附近的公交车站，除了请求的百度API与武汉智能公交后台API之外，本应用不向私有服务器上传数据，不获取您的隐私信息。
* APP需要的存储权限，为百度地图API的离线定位所用(经验证，不授予工作也正常)。
* APP首页下拉刷新，APP线路详情也为下拉刷新。
### 首页附近线路显示 
<img src="https://github.com/georgezzzh/miniBus/raw/master/readme_resources/Screenshot_20200824-112226.png" alt="首页显示附近线路"  height="800" />  

### 详细信息

<img alt="线路详细信息" src="https://github.com/georgezzzh/miniBus/raw/master/readme_resources/Screenshot_20200824-112300.png" height="800" />  

### 搜索发现
<img alt="搜索发现" src="https://github.com/georgezzzh/miniBus/raw/master/readme_resources/Screenshot_20200824-112324.png" height="800" />
