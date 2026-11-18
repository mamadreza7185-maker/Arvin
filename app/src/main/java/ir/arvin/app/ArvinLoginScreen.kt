package ir.arvin.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val midnight = Color(0xFF0C1931)
private val deepNavy = Color(0xFF172E50)
private val warmGold = Color(0xFFE7B44A)
private val lightGold = Color(0xFFFFE39A)
private val ivory = Color(0xFFFAF8F5)
private val fieldGrey = Color(0xFF677181)

@Composable
fun NativeLoginScreen(
    busy: Boolean,
    message: String?,
    savedUsername: String,
    rememberName: Boolean,
    canUseBiometrics: Boolean,
    hasBiometricLogin: Boolean,
    biometricUsername: String,
    onLogin: (String, String, Boolean, Boolean) -> Unit,
    onBiometricLogin: () -> Unit,
    onForgetLogin: () -> Unit
) {
    var username by remember(savedUsername) { mutableStateOf(savedUsername) }
    var password by remember { mutableStateOf("") }
    var saveName by remember(rememberName) { mutableStateOf(rememberName) }
    var enableBiometric by remember(hasBiometricLogin, canUseBiometrics) {
        mutableStateOf(hasBiometricLogin && canUseBiometrics)
    }
    var revealPassword by remember { mutableStateOf(false) }
    val canLogin = !busy && username.isNotBlank() && password.isNotBlank()
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(ivory).imePadding()
    ) {
        val compact = maxHeight < 760.dp
        val headerHeight = if (compact) 270.dp else 318.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth().height(headerHeight)) {
                Canvas(Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val lower = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w, h * 0.82f)
                        cubicTo(w * 0.72f, h * 1.00f, w * 0.32f, h * 0.98f, 0f, h * 0.83f)
                        close()
                    }
                    drawPath(
                        lower,
                        brush = Brush.verticalGradient(listOf(midnight, deepNavy, Color(0xFF0D1B35)))
                    )
                    val borderCurve = Path().apply {
                        moveTo(0f, h * 0.83f)
                        cubicTo(w * 0.32f, h * 0.98f, w * 0.72f, h * 1.00f, w, h * 0.82f)
                    }
                    drawPath(
                        borderCurve, brush = Brush.horizontalGradient(
                            listOf(Color(0xFFCF8D22), lightGold, Color(0xFFC18629))
                        ), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val upper = Path().apply {
                        moveTo(0f, h * 0.29f)
                        cubicTo(w * 0.27f, h * 0.03f, w * 0.59f, h * 0.03f, w, 0f)
                    }
                    drawPath(
                        upper, brush = Brush.horizontalGradient(
                            listOf(lightGold, warmGold, Color(0xFF97601C))
                        ), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(
                    modifier = Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth().padding(top = if (compact) 8.dp else 15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_arvin_logo),
                        contentDescription = "لوگوی پخش آروین",
                        modifier = Modifier.size(if (compact) 112.dp else 142.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("پخش آروین", color = Color(0xFFFFD77D),
                        fontSize = if (compact) 27.sp else 33.sp, fontWeight = FontWeight.ExtraBold)
                    Text("A R V I N   D I S T R I B U T I O N", color = Color.White,
                        fontSize = if (compact) 9.sp else 10.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Text("پخش مواد غذایی، بهداشتی و شوینده",
                        color = Color(0xFFF7F1E7), fontSize = 11.sp)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (compact) 2.dp else 5.dp))
                Text("پخش آروین", color = midnight, fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 29.sp else 34.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(5.dp))
                Text("سامانه حضور و غیاب و مدیریت کارکنان",
                    color = fieldGrey, fontSize = if (compact) 13.sp else 14.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(if (compact) 18.dp else 24.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(27.dp)),
                    shape = RoundedCornerShape(27.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0D99F))
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(
                            horizontal = 19.dp,
                            vertical = if (compact) 21.dp else 26.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = { Text("نام کاربری", color = fieldGrey) },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null,
                                tint = midnight) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(17.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = deepNavy,
                                unfocusedBorderColor = Color(0xFF718098),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("رمز عبور", color = fieldGrey) },
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null,
                                tint = midnight) },
                            trailingIcon = {
                                IconButton(onClick = { revealPassword = !revealPassword }) {
                                    Icon(
                                        imageVector = if (revealPassword) Icons.Filled.VisibilityOff
                                            else Icons.Filled.Visibility,
                                        contentDescription = if (revealPassword) "مخفی کردن رمز" else "نمایش رمز",
                                        tint = fieldGrey
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            visualTransformation = if (revealPassword)
                                VisualTransformation.None else PasswordVisualTransformation(),
                            shape = RoundedCornerShape(17.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = deepNavy,
                                unfocusedBorderColor = Color(0xFF718098),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = saveName,
                                onCheckedChange = { saveName = it },
                                colors = CheckboxDefaults.colors(checkedColor = deepNavy)
                            )
                            Text("ذخیره نام کاربری روی این گوشی",
                                color = midnight, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = enableBiometric,
                                enabled = canUseBiometrics && !busy,
                                onCheckedChange = {
                                    enableBiometric = it
                                    if (it) saveName = true
                                },
                                colors = CheckboxDefaults.colors(checkedColor = deepNavy)
                            )
                            Text("ذخیره رمز و ورود با اثر انگشت",
                                color = if (canUseBiometrics) midnight else fieldGrey,
                                fontSize = 13.sp)
                        }
                        if (!canUseBiometrics) {
                            Text(
                                "برای فعال‌سازی این گزینه، ابتدا اثر انگشت ایمن را در تنظیمات قفل گوشی ثبت کنید.",
                                color = fieldGrey, fontSize = 11.sp
                            )
                        } else if (enableBiometric && !hasBiometricLogin) {
                            Text(
                                "پس از یک ورود موفق با رمز، اثر انگشت را تأیید کنید تا رمز به‌صورت امن ذخیره شود.",
                                color = fieldGrey, fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier.fillMaxWidth().height(57.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.horizontalGradient(
                                    listOf(Color(0xFFD59B31), lightGold, Color(0xFFD8A033))
                                ))
                                .clickable(enabled = canLogin) {
                                    onLogin(username.trim(), password, saveName, enableBiometric)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (busy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp), strokeWidth = 2.dp,
                                        color = midnight
                                    )
                                } else {
                                    Icon(Icons.Filled.Login, contentDescription = null, tint = midnight)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(if (busy) "در حال ورود..." else "ورود به برنامه",
                                    color = midnight, fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        if (hasBiometricLogin && canUseBiometrics) {
                            OutlinedButton(
                                onClick = onBiometricLogin,
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, warmGold),
                                shape = RoundedCornerShape(17.dp)
                            ) {
                                Icon(Icons.Filled.Fingerprint, contentDescription = null,
                                    tint = midnight, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(9.dp))
                                Text("ورود با اثر انگشت", color = midnight,
                                    fontWeight = FontWeight.Bold)
                            }
                            Text("حساب ذخیره‌شده: $biometricUsername",
                                color = fieldGrey, fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center)
                        }
                        if (hasBiometricLogin || savedUsername.isNotBlank()) {
                            TextButton(
                                onClick = onForgetLogin,
                                enabled = !busy,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = null,
                                    tint = fieldGrey, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("حذف اطلاعات ورود ذخیره‌شده",
                                    fontSize = 11.sp, color = fieldGrey)
                            }
                        }
                        if (message != null) {
                            Text(message, color = Color(0xFFB42318), fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(Modifier.height(25.dp))
                Text(
                    "نسخه بومی اندروید  •  پخش مواد غذایی، بهداشتی و شوینده",
                    color = fieldGrey, fontSize = 10.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Canvas(Modifier.fillMaxWidth().height(37.dp)) {
                    val p = Path().apply {
                        moveTo(0f, size.height * 0.76f)
                        cubicTo(size.width * 0.35f, size.height * 0.1f,
                            size.width * 0.7f, size.height * 0.9f,
                            size.width, size.height * 0.42f)
                    }
                    drawPath(p, brush = Brush.horizontalGradient(
                        listOf(Color(0xFFAB771D), lightGold, Color(0xFFB57B1E))
                    ), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
        }
    }
}
