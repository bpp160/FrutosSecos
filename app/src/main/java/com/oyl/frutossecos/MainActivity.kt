package com.oyl.frutossecos

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private val Green=Color(0xFF17683D)
private val Light=Color(0xFFF2F8F2)
private val Orange=Color(0xFFE7A443)
private fun money(n:Int)="$"+NumberFormat.getIntegerInstance(Locale("es","CL")).format(n)
private val initial= listOf(
 ProductRow(1,"Almendras","Frutos secos","Almendras naturales seleccionadas",2990,25),
 ProductRow(2,"Nueces","Frutos secos","Nueces frescas en formato de 250 g",3490,18),
 ProductRow(3,"Mix Premium","Frutos secos","Mix de nueces, almendras y maní",3990,40),
 ProductRow(4,"Pasas","Deshidratados","Pasas sin semillas",2490,36),
 ProductRow(5,"Semillas de chía","Semillas","Semillas naturales para tus recetas",2990,22),
 ProductRow(6,"Cranberries","Deshidratados","Arándanos deshidratados",2890,16)
)

@OptIn(ExperimentalCoroutinesApi::class)
class OylViewModel(private val dao:StoreDao,private val prefs:android.content.SharedPreferences):ViewModel(){
 val products=dao.products().stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 var userId by mutableLongStateOf(prefs.getLong("userId",-1)); private set
 var userName by mutableStateOf(prefs.getString("name","")?:""); private set
 var isAdmin by mutableStateOf(prefs.getBoolean("admin",false)); private set
 val cart:StateFlow<List<CartRow>> = snapshotFlow { userId }.flatMapLatest { if(it>0) dao.cart(it) else flowOf(emptyList()) }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val orders:StateFlow<List<OrderRow>> = snapshotFlow { userId to isAdmin }.flatMapLatest { (id,admin) -> if(admin) dao.allOrders() else if(id>0) dao.clientOrders(id) else flowOf(emptyList()) }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 val notices:StateFlow<List<NoticeRow>> = snapshotFlow { userId }.flatMapLatest { dao.notices(if(it>0) it else 0) }.stateIn(viewModelScope,SharingStarted.Eagerly,emptyList())
 init { viewModelScope.launch { if(dao.productCount()==0) initial.forEach {dao.saveProduct(it)} } }
 fun register(name:String,alias:String,pass:String,phone:String,address:String,done:(String)->Unit){
  if(name.isBlank()||alias.isBlank()||pass.length<4){done("Completa los campos; contraseña mínima de 4 caracteres");return}
  viewModelScope.launch { if(dao.client(alias)!=null){done("El alias ya existe");return@launch}
   try{dao.addClient(ClientRow(name=name,alias=alias,password=pass,phone=phone,address=address));done("Cuenta creada. Inicia sesión")}
   catch(_:Exception){done("No se pudo crear el usuario")}
  }
 }
 fun login(alias:String,pass:String,admin:Boolean,done:(Boolean,String)->Unit){viewModelScope.launch {
  if(admin){if(alias=="admin"&&pass=="admin123") { session(0,"Administrador",true);done(true,"") } else done(false,"Credenciales de demostración incorrectas")}
  else {val c=dao.client(alias);if(c!=null&&c.password==pass){session(c.id,c.name,false);done(true,"")}else done(false,"Alias o contraseña incorrectos")}
 }}
 private fun session(id:Long,name:String,admin:Boolean){
  userId=id;userName=name;isAdmin=admin
  prefs.edit { putLong("userId",id); putString("name",name); putBoolean("admin",admin) }
 }
 fun logout(){session(-1,"",false)}
 fun addCart(p:ProductRow,qty:Int=1,done:(String)->Unit={}){ if(isAdmin){done("Inicia como cliente para comprar");return};viewModelScope.launch {
  val old=cart.value.find{it.productId==p.id}?.quantity?:0
  if(old+qty>p.stock){done("Stock insuficiente");return@launch}
  if(old+qty<=0)dao.removeCart(userId,p.id) else dao.saveCart(CartRow(userId,p.id,old+qty));done("Carrito actualizado")
 }}
 fun setCart(p:ProductRow,qty:Int){viewModelScope.launch { if(qty<=0)dao.removeCart(userId,p.id) else if(qty<=p.stock)dao.saveCart(CartRow(userId,p.id,qty)) }}
 fun placeOrder(delivery:String,done:(Long)->Unit){viewModelScope.launch {
  val lines=cart.value; val byId=products.value.associateBy{it.id}
  if(lines.isEmpty()){done(-1);return@launch}
  if(lines.any { (byId[it.productId]?.stock?:0)<it.quantity }) {done(-2);return@launch}
  val total=lines.sumOf{(byId[it.productId]?.price?:0)*it.quantity}
  // Demo offline: el servidor deberá validar precios y descontar stock de forma transaccional.
  val id=dao.addOrder(OrderRow(clientId=userId,total=total,delivery=delivery))
  lines.forEach{l->val p=byId[l.productId]!!;dao.addOrderLine(OrderLineRow(orderId=id,productId=p.id,name=p.name,quantity=l.quantity,unitPrice=p.price))}
  dao.clearCart(userId);dao.addNotice(NoticeRow(clientId=userId,title="Pedido recibido",message="Solicitud #$id pendiente de validación"));done(id)
 }}
 fun saveProduct(p:ProductRow){viewModelScope.launch{dao.saveProduct(p)}}
 fun stock(p:ProductRow,delta:Int){viewModelScope.launch{dao.saveProduct(p.copy(stock=(p.stock+delta).coerceAtLeast(0)))}}
 fun changeStatus(o:OrderRow,status:String){viewModelScope.launch{dao.status(o.id,status);dao.addNotice(NoticeRow(clientId=o.clientId,title="Estado de pedido",message="Pedido #${o.id}: $status"))}}
}
class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent {
 val context=LocalContext.current
 val vm:OylViewModel=viewModel(factory=object:ViewModelProvider.Factory{override fun <T:ViewModel> create(modelClass:Class<T>):T{
 @Suppress("UNCHECKED_CAST") return OylViewModel(OylDatabase.get(context).dao(),context.getSharedPreferences("oyl_session",MODE_PRIVATE)) as T
 }})
 OylApp(vm)
}}}
private enum class Screen(val title:String){WELCOME("Bienvenida"),REGISTER("Crear cuenta"),LOGIN("Iniciar sesión"),HOME("Inicio"),MAP("Mapa de tiendas"),CATALOG("Catálogo"),DETAIL("Detalle del producto"),QR("Escáner QR"),CART("Mi carrito"),CHECKOUT("Confirmar pedido"),MANAGE("Gestión de productos"),INVENTORY("Inventario"),ORDERS("Mis pedidos"),NOTICES("Notificaciones"),PROFILE("Mi perfil")}

@Composable private fun OylApp(vm:OylViewModel){
 var screen by remember { mutableStateOf(if(vm.userId>=0)Screen.HOME else Screen.WELCOME) }
 var selected by remember { mutableIntStateOf(1) }
 var orderConfirmed by remember { mutableLongStateOf(-1) }
 val products by vm.products.collectAsState()
 val cart by vm.cart.collectAsState()
 val orders by vm.orders.collectAsState()
 val notices by vm.notices.collectAsState()
 val context=LocalContext.current
 fun go(s:Screen){screen=s}
 fun toast(msg:String){Toast.makeText(context,msg,Toast.LENGTH_SHORT).show()}
 val nav= listOf(Screen.HOME,Screen.MAP,Screen.QR,Screen.ORDERS,Screen.PROFILE)
 MaterialTheme(colorScheme=lightColorScheme(primary=Green,secondary=Orange,background=Light,surface=Color.White)){
 Scaffold(topBar={if(screen !in listOf(Screen.WELCOME,Screen.LOGIN,Screen.REGISTER)){
  Surface(color=Green){Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("OYL  •  ${screen.title}",color=Color.White,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));Text(if(vm.isAdmin)"ADMIN" else "CLIENTE",color=Color.White)}}
 }},bottomBar={if(vm.userId>=0&&screen !in listOf(Screen.WELCOME,Screen.LOGIN,Screen.REGISTER)){
 NavigationBar {nav.forEachIndexed { i,s -> NavigationBarItem(selected=screen==s,onClick={go(s)},icon={Text(listOf("⌂","⌖","▣","▤","♙")[i])},label={Text(listOf("Inicio","Mapa","QR","Pedidos","Perfil")[i])}) }}
 }}) {pad-> Column(Modifier.fillMaxSize().padding(pad)){
 AnimatedContent(targetState=screen,label="screens") { target->
 when(target){
 Screen.WELCOME -> Welcome(onLogin={go(Screen.LOGIN)},onCatalog={go(Screen.CATALOG)},onMap={go(Screen.MAP)})
 Screen.REGISTER -> Register(onBack={go(Screen.LOGIN)},onSave={n,a,p,t,d->vm.register(n,a,p,t,d){toast(it);if(it.startsWith("Cuenta"))go(Screen.LOGIN)}})
 Screen.LOGIN -> Login(onRegister={go(Screen.REGISTER)},onLogin={a,p,admin->vm.login(a,p,admin){ok,msg->if(ok)go(Screen.HOME)else toast(msg)}})
 Screen.HOME -> Dashboard(vm,products,cart,orders,onGo=::go)
 Screen.MAP -> MapPage()
 Screen.CATALOG -> Catalog(products,vm.isAdmin,onSelect={selected=it.id;go(Screen.DETAIL)},onAdd={product->vm.addCart(product){msg->toast(msg)}})
 Screen.DETAIL -> ProductDetail(products.find{it.id==selected},vm.isAdmin,onBack={go(Screen.CATALOG)},onAdd={p,q->vm.addCart(p,q){toast(it)}})
 Screen.QR -> QRPage(onFound={code->val id=code.substringAfterLast("/").toIntOrNull();if(id!=null&&products.any{it.id==id}){selected=id;go(Screen.DETAIL)}else toast("QR no asociado a un producto")})
 Screen.CART -> CartPage(products,cart,onQuantity={p,q->vm.setCart(p,q)},onNext={go(Screen.CHECKOUT)})
 Screen.CHECKOUT -> CheckoutPage(products,cart,orderConfirmed,onConfirm={mode->vm.placeOrder(mode){id->if(id>0)orderConfirmed=id else toast(if(id == -2L)"Stock insuficiente" else "Carrito vacío")}},onOrders={orderConfirmed=-1;go(Screen.ORDERS)})
 Screen.MANAGE -> ManagePage(products,onSave={vm.saveProduct(it);toast("Producto guardado")})
 Screen.INVENTORY -> InventoryPage(products,onChange={p,d->vm.stock(p,d)})
 Screen.ORDERS -> OrdersPage(orders,vm.isAdmin,onStatus={o,s->vm.changeStatus(o,s)})
 Screen.NOTICES -> NoticesPage(notices)
 Screen.PROFILE -> ProfilePage(vm,onGo=::go,onLogout={vm.logout();go(Screen.WELCOME)})
 }
 }
 }}
 }
}
@Composable private fun Heading(title:String,sub:String="") {Text(title,style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=Green);if(sub.isNotBlank())Text(sub,color=Color.Gray)}
@Composable private fun Pill(label:String,action:()->Unit){Button(onClick=action,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){Text(label)}}
@Composable private fun Input(value:String,on:(String)->Unit,label:String,secret:Boolean=false){OutlinedTextField(value=value,onValueChange=on,label={Text(label)},modifier=Modifier.fillMaxWidth(),singleLine=true,visualTransformation=if(secret)PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)}
@Composable private fun Panel(content:@Composable ColumnScope.()->Unit){Card(Modifier.fillMaxWidth().padding(vertical=6.dp),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp),content=content)}}
@Composable private fun Welcome(onLogin:()->Unit,onCatalog:()->Unit,onMap:()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("🌿",style=MaterialTheme.typography.displayLarge);Text("OYL",style=MaterialTheme.typography.displayLarge,color=Green,fontWeight=FontWeight.Black);Text("FRUTOS SECOS",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Spacer(Modifier.height(20.dp));Text("Productos naturales más cerca de ti",textAlign=TextAlign.Center);Spacer(Modifier.height(28.dp));Panel{listOf("🥜 Catálogo actualizado","🛒 Pedidos online","📍 Retiros y despachos","🌱 Productos saludables").forEach{Text(it)}};Pill("Comenzar",onLogin);OutlinedButton(onClick=onLogin,modifier=Modifier.fillMaxWidth()){Text("Ya tengo una cuenta")};TextButton(onClick=onCatalog){Text("Ver catálogo de muestra")};TextButton(onClick=onMap){Text("Ubicación de tienda")}}}
@Composable private fun Register(onBack:()->Unit,onSave:(String,String,String,String,String)->Unit){var name by remember{mutableStateOf("")};var alias by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var address by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Heading("Crear cuenta","Registro de cliente");Input(name,{name=it},"Nombre completo");Input(alias,{alias=it},"Alias de acceso");Input(pass,{pass=it},"Contraseña",true);Input(phone,{phone=it},"Teléfono de prueba");Input(address,{address=it},"Dirección de prueba");Pill("Crear cuenta"){onSave(name,alias,pass,phone,address)};TextButton(onClick=onBack){Text("Volver a iniciar sesión")}}}
@Composable private fun Login(onRegister:()->Unit,onLogin:(String,String,Boolean)->Unit){var admin by remember{mutableStateOf(false)};var alias by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("🌰",style=MaterialTheme.typography.displayLarge);Heading("Iniciar sesión");Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(admin.not(),onClick={admin=false},label={Text("Cliente")});FilterChip(admin,onClick={admin=true},label={Text("Administrador")})};Input(alias,{alias=it},"Alias");Input(pass,{pass=it},"Contraseña",true);Pill("Iniciar sesión"){onLogin(alias,pass,admin)};if(admin)Text("Demo: alias admin / contraseña admin123",color=Color.Gray);TextButton(onClick=onRegister){Text("¿No tienes cuenta? Regístrate")}}}
@Composable private fun Dashboard(vm:OylViewModel,products:List<ProductRow>,cart:List<CartRow>,orders:List<OrderRow>,onGo:(Screen)->Unit){LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Heading("¡Hola, ${vm.userName}!","Qué bueno verte de nuevo")};item{Panel{Text("🌰 Compra saludable, vida mejor",fontWeight=FontWeight.Bold,color=Green);Text("Frutos secos, semillas y deshidratados para ti")}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Quick("🥜","Catálogo",Modifier.weight(1f)){onGo(Screen.CATALOG)};Quick("📍","Mapa",Modifier.weight(1f)){onGo(Screen.MAP)}}};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Quick("📦","Pedidos",Modifier.weight(1f)){onGo(Screen.ORDERS)};Quick("🔔","Avisos",Modifier.weight(1f)){onGo(Screen.NOTICES)}}};if(vm.isAdmin){item{Panel{Text("Panel administrador",fontWeight=FontWeight.Bold);Text("${products.count{it.stock<10}} productos con stock bajo · ${orders.count{it.status.startsWith("Pendiente")}} pedidos pendientes");Pill("Gestionar productos"){onGo(Screen.MANAGE)};Pill("Gestionar inventario"){onGo(Screen.INVENTORY)}}}}else{item{Panel{Text("Carrito: ${cart.sumOf{it.quantity}} artículos");Pill("Ir a mi carrito"){onGo(Screen.CART)}}}};item{Heading("Productos destacados")};items(products.take(4)){ProductCard(it,onOpen={onGo(Screen.CATALOG)},onAdd={onGo(Screen.CATALOG)})}}}
@Composable private fun Quick(icon:String,title:String,modifier:Modifier=Modifier,onClick:()->Unit){Card(onClick=onClick,modifier=modifier.height(90.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Text(icon,style=MaterialTheme.typography.headlineSmall);Text(title,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun ProductCard(p:ProductRow,onOpen:()->Unit,onAdd:()->Unit){Panel{Row(verticalAlignment=Alignment.CenterVertically){ProductPhoto(p,56);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.grams} g · ${p.category}",style=MaterialTheme.typography.bodySmall);Text(money(p.price),color=Green,fontWeight=FontWeight.Bold);Text("Stock: ${p.stock}",style=MaterialTheme.typography.labelSmall)};TextButton(onClick=onOpen){Text("Ver")};FilledIconButton(onClick=onAdd){Text("+")}}}}
@Composable private fun Catalog(products:List<ProductRow>,admin:Boolean,onSelect:(ProductRow)->Unit,onAdd:(ProductRow)->Unit){var search by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Todos")};Column(Modifier.padding(14.dp)){Heading("Catálogo de productos");Input(search,{search=it},"Buscar productos");Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Todos","Frutos secos","Semillas","Deshidratados").forEach{FilterChip(selected=cat==it,onClick={cat=it},label={Text(it)})}};LazyColumn{items(products.filter{it.active&&(cat=="Todos"||it.category==cat)&&it.name.contains(search,true)}){p->ProductCard(p,{onSelect(p)},{if(admin)onSelect(p)else onAdd(p)})}}}}
@Composable private fun ProductPhoto(p:ProductRow,size:Int){
 val bitmap=remember(p.photo){runCatching{if(p.photo.isNotBlank()&&File(p.photo).exists()) BitmapFactory.decodeFile(p.photo) else null}.getOrNull()}
 if(bitmap!=null) Image(bitmap.asImageBitmap(),contentDescription=p.name,modifier=Modifier.size(size.dp))
 else Text(when(p.category){"Semillas"->"🌱";"Deshidratados"->"🍇";else->"🥜"},style=MaterialTheme.typography.displaySmall)
}
@Composable private fun ProductDetail(p:ProductRow?,admin:Boolean,onBack:()->Unit,onAdd:(ProductRow,Int)->Unit){if(p==null){Text("Producto no encontrado");return};var qty by remember(p.id){mutableIntStateOf(1)};Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){TextButton(onClick=onBack){Text("← Catálogo")};Box(Modifier.fillMaxWidth().height(180.dp).background(Light,RoundedCornerShape(22.dp)),contentAlignment=Alignment.Center){ProductPhoto(p,145)};Heading(p.name,p.category);Text(money(p.price),style=MaterialTheme.typography.headlineSmall,color=Green);Text("${p.grams} g · ${p.description}");Text(if(p.stock>0)"● En stock (${p.stock})" else "Agotado",color=if(p.stock>0)Green else Color.Red);if(!admin){Row(verticalAlignment=Alignment.CenterVertically){OutlinedButton(onClick={qty=(qty-1).coerceAtLeast(1)}){Text("−")};Text("  $qty  ");OutlinedButton(onClick={qty=(qty+1).coerceAtMost(p.stock.coerceAtLeast(1))}){Text("+")}};Pill("Agregar al carrito"){onAdd(p,qty)}}else Text("Administración: usa Gestión de productos para editar")}}
@Composable private fun CartPage(products:List<ProductRow>,cart:List<CartRow>,onQuantity:(ProductRow,Int)->Unit,onNext:()->Unit){val byId=products.associateBy{it.id};val total=cart.sumOf{(byId[it.productId]?.price?:0)*it.quantity};LazyColumn(Modifier.padding(16.dp)){item{Heading("Mi carrito (${cart.sumOf{it.quantity}})")};items(cart){l->val p=byId[l.productId];if(p!=null)Panel{Row(verticalAlignment=Alignment.CenterVertically){Text("🥜",style=MaterialTheme.typography.headlineLarge);Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text(money(p.price))};TextButton(onClick={onQuantity(p,l.quantity-1)}){Text("−")};Text("${l.quantity}");TextButton(onClick={onQuantity(p,l.quantity+1)}){Text("+")}}}};item{Panel{Text("Total: ${money(total)}",fontWeight=FontWeight.Bold);Pill("Continuar compra",onNext)}}}}
@Composable private fun CheckoutPage(products:List<ProductRow>,cart:List<CartRow>,confirmed:Long,onConfirm:(String)->Unit,onOrders:()->Unit){var mode by remember{mutableStateOf("Retiro en tienda")};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){if(confirmed>0){Text("🎉",style=MaterialTheme.typography.displayLarge);Heading("¡Pedido registrado!");Text("N.º de solicitud: #$confirmed");Text("Estado: Pendiente de validación");Text("Modo demostración: no se ha enviado al servidor ni reservado inventario.",color=Color.Gray);Pill("Ver mis pedidos",onOrders)}else{Heading("Confirmación del pedido");Text("Total: ${money(cart.sumOf{l->(products.find{it.id==l.productId}?.price?:0)*l.quantity})}");listOf("Retiro en tienda","Entrega a domicilio").forEach{Row(verticalAlignment=Alignment.CenterVertically){RadioButton(selected=mode==it,onClick={mode=it});Text(it)}};Pill("Confirmar solicitud"){onConfirm(mode)};Text("Demo local. El backend deberá verificar stock y precios antes de aceptar el pedido.",color=Color.Gray)}}}
@Composable private fun ManagePage(products:List<ProductRow>,onSave:(ProductRow)->Unit){var editing by remember{mutableIntStateOf(0)};var name by remember{mutableStateOf("")};var cat by remember{mutableStateOf("Frutos secos")};var desc by remember{mutableStateOf("")};var price by remember{mutableStateOf("")};var stock by remember{mutableStateOf("")};var active by remember{mutableStateOf(true)};var photo by remember{mutableStateOf("")}
 val context=LocalContext.current
 val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->
 if(bitmap!=null){val file=File(context.filesDir,"product_${System.currentTimeMillis()}.jpg");file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG,85,it)};photo=file.absolutePath}
 }
 val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)camera.launch(null)}
 Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Heading("Gestión de productos","Administrador");products.forEach{p->Panel{Row(verticalAlignment=Alignment.CenterVertically){Text(p.name,modifier=Modifier.weight(1f));Text(money(p.price));TextButton(onClick={editing=p.id;name=p.name;cat=p.category;desc=p.description;price=p.price.toString();stock=p.stock.toString();active=p.active;photo=p.photo}){Text("Editar")}}}};HorizontalDivider();Heading(if(editing==0)"Nuevo producto" else "Editar producto");Input(name,{name=it},"Nombre");Input(cat,{cat=it},"Categoría");Input(desc,{desc=it},"Descripción");Input(price,{price=it},"Precio");Input(stock,{stock=it},"Stock");OutlinedButton(onClick={permission.launch(Manifest.permission.CAMERA)}){Text("📷 Fotografiar producto")};if(photo.isNotBlank())Text("Fotografía guardada localmente");Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=active,onCheckedChange={active=it});Text("Producto activo")};Pill("Guardar producto"){val pr=price.toIntOrNull();val st=stock.toIntOrNull();if(name.isNotBlank()&&pr!=null&&pr>=0&&st!=null&&st>=0){onSave(ProductRow(if(editing==0)(products.maxOfOrNull{it.id}?:0)+1 else editing,name,cat,desc,pr,st,active=active,photo=photo));editing=0;name="";desc="";price="";stock="";photo=""}}}}
@Composable private fun InventoryPage(products:List<ProductRow>,onChange:(ProductRow,Int)->Unit){Column(Modifier.padding(16.dp)){Heading("Gestión de inventario","Movimientos de demostración");LazyColumn{items(products){p->Panel{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("Stock actual: ${p.stock}"+(if(p.stock<10)" · BAJO" else ""))};FilledTonalButton(onClick={onChange(p,-1)}){Text("−1")};Spacer(Modifier.width(6.dp));FilledTonalButton(onClick={onChange(p,1)}){Text("+1")}}}}}}}
@Composable private fun OrdersPage(orders:List<OrderRow>,admin:Boolean,onStatus:(OrderRow,String)->Unit){var filter by remember{mutableStateOf("Todos")};Column(Modifier.padding(16.dp)){Heading(if(admin)"Gestión de pedidos" else "Mis pedidos");Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Todos","Pendiente","Confirmado","Preparado","Entregado").forEach{FilterChip(selected=filter==it,onClick={filter=it},label={Text(it)})}};LazyColumn{items(orders.filter{filter=="Todos"||it.status.startsWith(filter)}){o->Panel{Text("Pedido #${o.id}",fontWeight=FontWeight.Bold);Text("${money(o.total)} · ${o.delivery}");Text("Estado: ${o.status}",color=Green);if(admin){Row(Modifier.horizontalScroll(rememberScrollState())){listOf("Confirmado","Preparado","Entregado").forEach{s->TextButton(onClick={onStatus(o,s)}){Text(s)}}}}}}}}}
@Composable private fun NoticesPage(notices:List<NoticeRow>){LazyColumn(Modifier.padding(16.dp)){item{Heading("Notificaciones")};items(notices){n->Panel{Text("🔔 ${n.title}",fontWeight=FontWeight.Bold);Text(n.message)}};if(notices.isEmpty())item{Text("No hay avisos todavía")}}}
@Composable private fun ProfilePage(vm:OylViewModel,onGo:(Screen)->Unit,onLogout:()->Unit){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),horizontalAlignment=Alignment.CenterHorizontally){Text("👤",style=MaterialTheme.typography.displayLarge);Heading(vm.userName,if(vm.isAdmin)"Administrador" else "Cliente");Panel{Text("Preferencias y accesos");listOf("Catálogo" to Screen.CATALOG,"Carrito" to Screen.CART,"Notificaciones" to Screen.NOTICES,"Pedidos" to Screen.ORDERS).forEach{(t,s)->TextButton(onClick={onGo(s)}){Text("→ $t")}};if(vm.isAdmin){TextButton(onClick={onGo(Screen.MANAGE)}){Text("→ Gestión de productos")};TextButton(onClick={onGo(Screen.INVENTORY)}){Text("→ Inventario")}}};Pill("Cerrar sesión",onLogout)}}
@Composable private fun MapPage(){val context=LocalContext.current;Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Heading("Mapa de tiendas","Puntos de retiro de demostración");Panel{Text("📍 Tienda OYL — Santiago Centro",fontWeight=FontWeight.Bold);Text("Santiago, Región Metropolitana · Ubicación referencial");Text("Horario: consultar antes de visitar");Pill("Abrir mapa"){val intent=Intent(Intent.ACTION_VIEW,Uri.parse("geo:0,0?q=Santiago+Centro+Chile"));runCatching{context.startActivity(intent)}}};Text("El mapa se abre en una aplicación instalada. La ubicación es de prueba y debe sustituirse por la dirección real de la tienda.",color=Color.Gray)}}
@Composable private fun QRPage(onFound:(String)->Unit){val context=LocalContext.current;var scanned by remember{mutableStateOf("")};var manual by remember{mutableStateOf("")};val scanner=remember{BarcodeScanning.getClient()};val launcher=rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()){bitmap->if(bitmap!=null){scanner.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener{list->val value=list.firstOrNull()?.rawValue;if(value!=null){scanned=value;onFound(value)}else Toast.makeText(context,"No se encontró QR; prueba con más luz",Toast.LENGTH_LONG).show()}.addOnFailureListener{Toast.makeText(context,"Error al leer QR",Toast.LENGTH_SHORT).show()}}};val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){ok->if(ok)launcher.launch(null)else Toast.makeText(context,"Se necesita permiso de cámara",Toast.LENGTH_SHORT).show()};Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),horizontalAlignment=Alignment.CenterHorizontally){Heading("Escáner QR de productos");Text("▣",style=MaterialTheme.typography.displayLarge);Text("Fotografía el código QR del producto");Pill("Abrir cámara"){permission.launch(Manifest.permission.CAMERA)};Input(manual,{manual=it},"ID de producto (prueba: 1 a 6)");Pill("Consultar código manual"){onFound(manual)};if(scanned.isNotBlank())Text("Leído: $scanned");Text("Usa códigos que contengan el ID numérico del producto o terminen en /ID.",color=Color.Gray)}}
