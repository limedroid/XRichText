# XRichText

---

[XRichText](https://github.com/limedroid/XRichText.git)是一个可以显示Html富文本的TextView。可以用于显示新闻、商品详情等场景。欢迎star、fork，提出意见。

<p align="center">
  <img src="art/xrichtext.gif" alt="XRecyclerView" />
</p>

## 使用

* Github : [XRichText](https://github.com/limedroid/XRichText.git)

### step1 

在根项目的`build.gradle`文件中添加

```groovy
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
	}
}
```

### step2

添加依赖

```groovy
dependencies {
	    compile 'com.github.limedroid:XRichText:v1.0.0'
}
```


## 特别说明:
**可以直接调用text方法显示html，其他的接口只是应对特殊场景的。**

## 主要特性 

* 自定义超链接link的点击
* 自定义图片img的点击
* 支持链式调用
* 图片支持三种对齐方式(左对齐、居中、右对齐)
* 支持在图片加载前对每张图片的url、width、height、对齐方式精细调整
* 内置图片下载器
* 可自定义图片下载器，如使用universal image loader、Picasso、Glide等

## 实现原理

* spanned
* 线程池
* 自定义ImageGetter

## 示例
```xml
<cn.droidlover.xrichtext.XRichText
            android:id="@+id/richText"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="16dp" />
```

在Java中：
```java
richText
                .callback(new XRichText.BaseClickCallback() {

                    @Override
                    public boolean onLinkClick(String url) {
                        showMsg(url);
                        return true;
                    }

                    @Override
                    public void onImageClick(List<String> urlList, int position) {
                        super.onImageClick(urlList, position);
                        showMsg("图片:" + position);
                    }

                    @Override
                    public void onFix(XRichText.ImageHolder holder) {
                        super.onFix(holder);
                        if (holder.getPosition() % 3 == 0) {
                            holder.setStyle(XRichText.Style.LEFT);
                        } else if (holder.getPosition() % 3 == 1) {
                            holder.setStyle(XRichText.Style.CENTER);
                        } else {
                            holder.setStyle(XRichText.Style.RIGHT);
                        }

                        //设置宽高
                        holder.setWidth(550);
                        holder.setHeight(400);
                    }
                })
               .imageDownloader(new ImageLoader() {
                   @Override
                   public Bitmap getBitmap(String url) throws IOException {
                        return UILKit.getLoader().loadImageSync(url);
                   }
               })
                .text(TEXT);
```

## api说明

* onLinkClick(String url) 当点击超链接时触发，url为点击的url
* onImageClick(List<String> urlList, int position) 当点击图片时触发，urlList为图片的url集合，position为被点击的位置，从0开始
* onFix(XRichText.ImageHolder holder) 当图片加载前回调此方法，通过holder可以调整图片的src、width、height、style(对齐方式)
* **设置html内容时，务必调用text方法**
* imageDownloader(ImageLoader loader)可以自定义图片加载器,库中已有默认实现。可以根据项目情况定义加载器，如三方库UIL、Picasso等，只需实现ImageLoader接口就行。**getBitmap方法已经在线程池中，所以自定义loader时不必考虑线程问题.**
* ClickCallback接口有默认实现类BaseClickCallback,可以直接使用此类重写需要的方法。


> 我的另一个项目: [**XDroid**](https://github.com/limedroid/XDroid) ， 一个轻量级的Android快速开发框架





