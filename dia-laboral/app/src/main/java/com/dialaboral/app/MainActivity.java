package com.dialaboral.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
  LinearLayout root, list; EditText nameInput, priceInput; TextView totalView; boolean spanish=false;
  JSONArray boxes=new JSONArray(), days=new JSONArray(); HashMap<String,EditText> quantities=new HashMap<>();
  android.content.SharedPreferences prefs;
  int orange=Color.rgb(255,152,0), dark=Color.rgb(20,28,43);
  public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("dia",0); try{boxes=new JSONArray(prefs.getString("boxes","[]"));days=new JSONArray(prefs.getString("days","[]"));}catch(Exception e){}
    if(boxes.length()==0){addBoxData("A",2.5);addBoxData("B",3.0);addBoxData("C",3.5);} render();}
  void addBoxData(String n,double p){JSONObject o=new JSONObject();try{o.put("name",n);o.put("price",p);boxes.put(o);}catch(Exception e){} save();}
  void save(){prefs.edit().putString("boxes",boxes.toString()).putString("days",days.toString()).apply();}
  TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(8,8,8,8);return t;}
  LinearLayout vertical(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  EditText edit(String hint,boolean num){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(hint);e.setTextColor(Color.WHITE);e.setHintTextColor(0xffaab4c5);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(orange)); if(num)e.setInputType(8194);return e;}
  Button btn(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setOnClickListener(v->r.run());return b;}
  void render(){root=vertical();root.setPadding(18,12,18,18);root.setBackgroundColor(dark); ScrollView sc=new ScrollView(this);sc.setFillViewport(true); LinearLayout page=vertical(); page.setPadding(0,0,0,18);
    TextView title=text("🪓 🔨 día laboral",26);title.setTypeface(null,Typeface.BOLD);title.setTextColor(orange);page.addView(title);
    page.addView(btn(spanish?"Cambiar a árabe":"التبديل إلى الإسبانية / Cambiar idioma",()->{spanish=!spanish;render();}));
    page.addView(text(spanish?"Cajas y precios":"الصناديق والأثمنة",19));
    list=vertical(); page.addView(list);
    for(int i=0;i<boxes.length();i++){JSONObject o=boxes.optJSONObject(i);if(o==null)continue;String n=o.optString("name");double p=o.optDouble("price");LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
      TextView label=text(n+"  ·  "+String.format(Locale.US,"%.2f €",p),17);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
      row.addView(btn(spanish?"Editar":"تعديل",()->editBox(n,p)));
      EditText q=edit(spanish?"Cantidad":"العدد",true);q.setText("".equals(q.getText().toString())?"":q.getText().toString());q.setEms(4); quantities.put(n,q);row.addView(q);list.addView(row);
      q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){calculate();}public void afterTextChanged(android.text.Editable e){}});
    }
    nameInput=edit(spanish?"New box letter/name":"اسم الصندوق الجديد (A, B, C...)",false);page.addView(nameInput);
    priceInput=edit(spanish?"Price per box (€)":"ثمن الصندوق (€)",true);page.addView(priceInput);
    page.addView(btn(spanish?"Add box":"إضافة صندوق",()->{String n=nameInput.getText().toString().trim().toUpperCase(Locale.ROOT);String ps=priceInput.getText().toString().trim().replace(',','.');if(n.isEmpty()||ps.isEmpty()){toast(spanish?"Enter name and price":"دخل اسم الصندوق والثمن");return;}for(int i=0;i<boxes.length();i++)if(boxes.optJSONObject(i).optString("name").equals(n)){toast(spanish?"Box already exists":"هاد الصندوق موجود");return;}try{addBoxData(n,Double.parseDouble(ps));render();}catch(Exception e){toast("Invalid price");}}));
    totalView=text("",24);totalView.setTextColor(orange);page.addView(totalView);calculate();
    page.addView(btn(spanish?"Save today's work":"حفظ نهار الخدمة",()->saveDay()));
    page.addView(btn(spanish?"Export Excel-compatible CSV":"تحميل البيانات لملف Excel (CSV)",()->exportCsv()));
    page.addView(text(spanish?"Saved days":"سجل الأيام",19));
    for(int i=days.length()-1;i>=0;i--){JSONObject d=days.optJSONObject(i);if(d!=null)page.addView(text(d.optString("date")+"  —  "+String.format(Locale.US,"%.2f €",d.optDouble("total")),16));}
    sc.addView(page);root.addView(sc);setContentView(root);
  }
  void calculate(){double total=0;for(int i=0;i<boxes.length();i++){JSONObject o=boxes.optJSONObject(i);if(o==null)continue;EditText q=quantities.get(o.optString("name"));if(q!=null){try{total+=Double.parseDouble(q.getText().toString())*o.optDouble("price");}catch(Exception e){}}}if(totalView!=null)totalView.setText((spanish?"TOTAL: ":"المجموع: ")+String.format(Locale.US,"%.2f €",total));}
  void editBox(String name,double price){LinearLayout l=vertical();EditText p=edit("Precio / الثمن",true);p.setText(String.valueOf(price));new AlertDialog.Builder(this).setTitle((spanish?"Edit ":"تعديل ")+name).setView(p).setPositiveButton("OK",(d,w)->{try{double np=Double.parseDouble(p.getText().toString().replace(',','.'));for(int i=0;i<boxes.length();i++){JSONObject o=boxes.optJSONObject(i);if(o.optString("name").equals(name))o.put("price",np);}save();render();}catch(Exception e){toast("Invalid price");}}).setNegativeButton(spanish?"Cancel":"إلغاء",null).show();}
  void saveDay(){try{JSONObject d=new JSONObject();String date=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());d.put("date",date);double total=0;JSONObject counts=new JSONObject(),prices=new JSONObject();for(int i=0;i<boxes.length();i++){JSONObject o=boxes.optJSONObject(i);String n=o.optString("name");EditText q=quantities.get(n);int qty=0;try{qty=Integer.parseInt(q.getText().toString());}catch(Exception e){}counts.put(n,qty);prices.put(n,o.optDouble("price"));total+=qty*o.optDouble("price");if(q!=null)q.setText("");}d.put("counts",counts);d.put("prices",prices);d.put("total",total);days.put(d);save();render();toast(spanish?"Day saved":"تحفظ نهار الخدمة");}catch(Exception e){toast("Error");}}
  void exportCsv(){try{StringBuilder s=new StringBuilder("Date;");for(int i=0;i<boxes.length();i++)s.append(boxes.optJSONObject(i).optString("name")).append(" qty;").append(boxes.optJSONObject(i).optString("name")).append(" price;");s.append("Total\n");for(int i=0;i<days.length();i++){JSONObject d=days.optJSONObject(i),c=d.optJSONObject("counts"),p=d.optJSONObject("prices");s.append(d.optString("date")).append(';');for(int j=0;j<boxes.length();j++){String n=boxes.optJSONObject(j).optString("name");s.append(c.optInt(n)).append(';').append(p.optDouble(n)).append(';');}s.append(d.optDouble("total")).append('\n');}
      Intent in=new Intent(Intent.ACTION_CREATE_DOCUMENT);in.addCategory(Intent.CATEGORY_OPENABLE);in.setType("text/csv");in.putExtra(Intent.EXTRA_TITLE,"dia_laboral.csv");startActivityForResult(in,7);pendingCsv=s.toString();
    }catch(Exception e){toast("Export error");}}
  String pendingCsv="";
  protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==7&&res==RESULT_OK&&data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(("\uFEFF"+pendingCsv).getBytes("UTF-8"));toast(spanish?"Export complete":"تم تحميل البيانات");}catch(Exception e){toast("Save error");}}}
  void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}