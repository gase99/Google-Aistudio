<div dir="rtl" align="right">

# 🪚 A-Cut 2Dpro (نسخهٔ بومی اندروید)

### ورق را کامل ببر، پرت را به حداقل برسان.

**بهینه‌ساز هوشمند برش دوبعدی ورق** برای کارگاه‌های چوب، MDF، شیشه و فلز — پیاده‌سازی بومی اندروید با **Kotlin** و **Jetpack Compose (Material 3)**، کاملاً فارسی، راست‌چین و **آفلاین**.

---

## ✨ امکانات کلیدی

1. **تعریف ورق و قطعات (`SetupScreen`)**
   - تنظیم ابعاد ورق خام (پریست‌های ۳۶۶۰×۱۸۳۰، ۲۴۴۰×۱۲۲۰، ۲۸۰۰×۲۰۷۰ یا ابعاد دلخواه)، ضخامت تیغه (Kerf)، ضخامت نوار PVC و مجاز بودن چرخش ۹۰°.
   - ورود قطعات با پشتیبانی از ارقام فارسی/عربی (`۰-۹` / `٠-٩`)، پیش‌نمایش زندهٔ قطعه (`PartPreviewCanvas`) و کنترل عملیات لبه‌ها برای **نوار**، **فارسی ۴۵°** و **شیار** (۰ / ۱ / ۲ لبه در طول و عرض).
   - محاسبهٔ زندهٔ متراژ نوار، طول شیار، تعداد لبه‌های فارسی ۴۵° و مساحت کل.

2. **الگوریتم چیدمان گیلوتینی (`GuillotinePacker`)**
   - چیدمان با معیار Best Area Fit / Short Side Fit / Long Side Fit و ادغام مستطیل‌های آزاد.
   - محاسبهٔ دقیق خوراک تیغه (Kerf) و کسر ضخامت نوار پیش از برش.

3. **تحلیل، نقشهٔ برش و چک‌لیست تولید (`AnalysisScreen` & `CuttingMapCanvas`)**
   - کارت‌های آمار کلیدی (تعداد ورق، بهره‌وری، پرت، قطعات چیده‌شده)، نمایشگر JSON و اخطار قطعات بزرگ‌تر از ورق.
   - نقشهٔ سیاه‌وسفید کارگاهی با هاشور ضایعات، علائم عملیات روی هر لبه (`ن`، `ش`، `ف`)، برچسب ابعاد، زوم ۵۰٪ تا ۳۰۰٪ و لمس قطعه برای مشاهدهٔ جزئیات.
   - برگهٔ کنترل تولید با چک‌لیست تعاملی و فاکتور تخمینی متریال مصرفی.

4. **برگهٔ لیبل قطعات (`LabelsScreen`)**
   - پیش‌نمایش ۳×۵ صفحه‌بندی‌شدهٔ A4 (۱۵ لیبل در هر برگه) به‌همراه نمای کارتی موبایل و نمودار جهت‌نمای لبه‌ها (`MiniPartDiagramCanvas`).

5. **خروجی‌های کارگاهی (`ExportsScreen`)**
   - تولید بومی **PDF A4 نقشهٔ برش + جدول گروه‌بندی‌شدهٔ قطعات + فاکتور متریال** (`PdfDocument`) با امکان اشتراک‌گذاری (`FileProvider`)، ذخیره در دستگاه (SAF) و چاپ مستقیم (`PrintManager`).
   - تولید بومی **PDF لیبل‌های ۳×۵ در A4** با اشتراک‌گذاری، ذخیره و چاپ.
   - تولید فایل استاندارد **DXF R12 (`AC1009`)** با لایه‌های `SHEET`، `CUT`، `GROOVE`، `FARSI_45` و `LABEL`.

6. **مدیریت محلی پروژه‌ها (Room + SharedPreferences)**
   - ذخیرهٔ خودکار پیش‌نویس فضای کاری، ذخیره/Save As، جستجو، بازکردن و حذف پروژه‌ها در دیتابیس محلی Room (`acut-2dpro-db`) بدون نیاز به اینترنت یا حساب کاربری.

---

## 🧩 ساختار کد

```text
app/src/main/java/com/acut/cut2dpro/
  MainActivity.kt
  domain/
    model/       # CutPart، ProjectModels، ChecklistHelper
    algorithm/   # GuillotinePacker، PartEdges، CuttingMapGeometry
    export/      # DxfExporter، PdfExporter
  data/
    local/       # AppDatabase (Room)، ProjectDao، ProjectEntity، WorkspaceDraftStore
    repository/  # ProjectRepository
  ui/
    MainViewModel.kt
    theme/       # Color.kt، Theme.kt (RTL)
    components/  # TopHeaderBar، SetupScreen، PartPreviewCanvas، CuttingMapCanvas، AnalysisScreen، LabelsScreen، ExportsScreen
```

---

## 📄 لایسنس

این پروژه تحت **MIT License** منتشر شده است. متن کامل در فایل [`LICENSE`](LICENSE) آمده است.

</div>
