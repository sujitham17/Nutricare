package com.nutricare.nutricarebackend.config;

import com.nutricare.nutricarebackend.entity.Disease;
import com.nutricare.nutricarebackend.repository.DiseaseRepository;
import com.nutricare.nutricarebackend.service.DiseaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DefaultDiseaseSeeder implements ApplicationRunner {

    private final DiseaseRepository diseaseRepository;
    private final DiseaseService diseaseService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (diseaseRepository.count() > 0) {
            return;
        }

        // 1. Diabetes
        seed("Diabetes", "Droplets", "#ef4444", "bg-red-50",
                "https://images.unsplash.com/photo-1579621970563-ebec7560ff3e?w=400&q=80&auto=format&fit=crop",
                "Effective blood sugar management through personalized low-glycemic nutrition strategies.",
                "Diabetes is a chronic condition where the body cannot properly process blood sugar. Type 1 is autoimmune, Type 2 is largely lifestyle-related and preventable.",
                List.of("Frequent urination", "Excessive thirst", "Unexplained weight loss", "Blurred vision", "Slow-healing wounds"),
                List.of("Low glycemic foods — oats, legumes, leafy greens", "High fiber diet to slow glucose absorption", "Lean proteins like chicken, fish, tofu", "Healthy fats from avocado and nuts"),
                List.of("Sugary drinks and sodas", "White rice and refined flour", "Fried and processed foods", "High-sugar fruits in excess"),
                "Eating smaller, frequent meals throughout the day helps maintain stable blood sugar levels.");

        // 2. Hypertension
        seed("Hypertension", "Heart", "#f97316", "bg-orange-50",
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&q=80&auto=format&fit=crop",
                "Reduce sodium intake and incorporate heart-healthy minerals to stabilize blood pressure.",
                "Hypertension is when blood force against artery walls is consistently too high, increasing risk of heart disease and stroke.",
                List.of("Headaches", "Shortness of breath", "Nosebleeds", "Chest pain", "Vision problems"),
                List.of("DASH diet — fruits, vegetables, whole grains", "Potassium-rich foods like bananas and sweet potatoes", "Low-fat dairy for calcium and magnesium", "Fatty fish for omega-3 fatty acids"),
                List.of("High sodium foods and table salt", "Processed meats and canned foods", "Alcohol and caffeine in excess", "Full-fat dairy and red meat"),
                "The DASH diet can reduce blood pressure by up to 11 mm Hg within weeks.");

        // 3. PCOS
        seed("PCOS", "Activity", "#ec4899", "bg-pink-50",
                "https://images.unsplash.com/photo-1535914254981-b5012eebbd15?w=400&q=80&auto=format&fit=crop",
                "Balance hormones and improve insulin sensitivity through anti-inflammatory dietary plans.",
                "Polycystic Ovary Syndrome is a hormonal disorder causing enlarged ovaries with small cysts, affecting menstrual cycles and fertility.",
                List.of("Irregular periods", "Excess hair growth", "Acne", "Weight gain", "Hair thinning"),
                List.of("Anti-inflammatory foods — berries, turmeric, ginger", "High fiber vegetables to improve insulin sensitivity", "Lean proteins and healthy fats", "Magnesium-rich foods like spinach and almonds"),
                List.of("Refined carbohydrates", "Sugary foods and drinks", "Inflammatory processed snacks", "Excess dairy for some individuals"),
                "Even a 5–10% reduction in body weight can significantly improve hormonal balance in PCOS.");

        // 4. Obesity
        seed("Obesity", "Weight", "#8b5cf6", "bg-violet-50",
                "https://images.unsplash.com/photo-1538805060514-97d9cc17730c?w=400&q=80&auto=format&fit=crop",
                "Scientific weight management focusing on metabolic health and sustainable caloric control.",
                "Obesity is a complex condition involving excessive body fat that increases risk of heart disease, diabetes, and certain cancers.",
                List.of("Excess body fat accumulation", "Breathlessness", "Increased sweating", "Joint pain", "Low energy and fatigue"),
                List.of("Calorie-deficit diet with whole foods", "High protein to preserve muscle mass", "Fiber-rich vegetables and legumes", "Adequate hydration — 2.5 to 3 liters daily"),
                List.of("Ultra-processed foods and fast food", "Sugary beverages including fruit juices", "Large portion sizes", "Late-night eating habits"),
                "Sustainable weight loss of 0.5–1 kg per week is healthier than crash dieting.");

        // 5. Thyroid
        seed("Thyroid", "AlertCircle", "#06b6d4", "bg-cyan-50",
                "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400&q=80&auto=format&fit=crop",
                "Supporting metabolic function with iodine, selenium, and nutrient-dense foods.",
                "Thyroid disorders affect the gland's ability to produce hormones that regulate metabolism, energy, and organ function.",
                List.of("Unexplained weight changes", "Fatigue and weakness", "Temperature sensitivity", "Hair loss", "Mood changes"),
                List.of("Iodine-rich foods — seaweed and seafood", "Selenium sources — Brazil nuts, sunflower seeds", "Zinc from pumpkin seeds and chickpeas", "Antioxidant-rich berries and vegetables"),
                List.of("Raw goitrogenic vegetables in excess", "Soy products in large quantities", "Gluten if sensitivity exists", "Highly processed foods"),
                "Cooking goitrogenic vegetables deactivates compounds that interfere with thyroid function.");

        // 6. Cholesterol
        seed("Cholesterol", "BarChart2", "#f59e0b", "bg-amber-50",
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&q=80&auto=format&fit=crop",
                "Manage lipid profiles with high-fiber diets and healthy unsaturated fats.",
                "High cholesterol means too much fatty substance in the blood, increasing risk of heart disease and stroke.",
                List.of("Usually no symptoms", "Chest pain in severe cases", "Yellowish deposits around eyes", "Detected through blood tests"),
                List.of("Soluble fiber from oats, barley, and beans", "Omega-3 fatty acids from salmon and flaxseeds", "Plant sterols from fortified foods", "Almonds and walnuts"),
                List.of("Trans fats in fried and packaged foods", "Saturated fats from red meat and full-fat dairy", "Refined carbohydrates and sugar", "Coconut oil and palm oil in excess"),
                "Eating 5–10 grams of soluble fiber daily can reduce LDL cholesterol by up to 5%.");

        // 7. Heart Disease
        seed("Heart Disease", "HeartPulse", "#ef4444", "bg-red-50",
                "https://images.unsplash.com/photo-1505751172876-fa1923c5c528?w=400&q=80&auto=format&fit=crop",
                "Comprehensive nutrition strategies to support long-term cardiovascular strength.",
                "Heart disease encompasses conditions affecting heart structure including coronary artery disease, arrhythmias, and heart failure.",
                List.of("Chest pain or discomfort", "Shortness of breath", "Fatigue", "Irregular heartbeat", "Swelling in legs"),
                List.of("Mediterranean diet — olive oil, fish, vegetables", "Whole grains and legumes", "Antioxidant-rich berries and dark chocolate", "Low-sodium, high-potassium foods"),
                List.of("Saturated and trans fats", "High sodium processed foods", "Alcohol in excess", "Red and processed meats"),
                "The Mediterranean diet is clinically proven to reduce cardiovascular disease risk by over 30%.");

        // 8. Anemia
        seed("Anemia", "Zap", "#dc2626", "bg-red-50",
                "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=400&q=80&auto=format&fit=crop",
                "Boost hemoglobin levels with iron-rich diets and optimal vitamin C absorption.",
                "Anemia means lacking enough healthy red blood cells to carry adequate oxygen to body tissues, causing fatigue and weakness.",
                List.of("Extreme fatigue", "Pale skin", "Shortness of breath", "Dizziness", "Cold hands and feet"),
                List.of("Iron-rich foods — red meat, lentils, spinach, tofu", "Vitamin C foods to enhance iron absorption", "Folate from leafy greens and fortified cereals", "Vitamin B12 from eggs, dairy, and meat"),
                List.of("Tea and coffee with meals", "Calcium-rich foods at same time as iron-rich foods", "Excess fiber that reduces iron absorption"),
                "Eating vitamin C alongside iron-rich foods can increase iron absorption by up to 300%.");

        // 9. Arthritis
        seed("Arthritis", "Bone", "#2563eb", "bg-blue-50",
                "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&q=80&auto=format&fit=crop",
                "Reduce joint inflammation and pain through Omega-3 and antioxidant-rich foods.",
                "Arthritis is joint inflammation causing pain and stiffness. Osteoarthritis involves cartilage breakdown while rheumatoid arthritis is autoimmune.",
                List.of("Joint pain and stiffness", "Swelling around joints", "Reduced range of motion", "Warmth and redness", "Morning stiffness"),
                List.of("Omega-3 fatty acids from fatty fish", "Antioxidants from colorful vegetables and fruits", "Turmeric and ginger for natural anti-inflammation", "Vitamin D from fortified foods"),
                List.of("Processed and fried foods", "Red meat and high-fat dairy", "Sugar and refined carbohydrates", "Alcohol and tobacco"),
                "Curcumin in turmeric has been shown to be as effective as some anti-inflammatory medications.");

        // 10. Fatty Liver
        seed("Fatty Liver", "Activity", "#d97706", "bg-yellow-50",
                "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400&q=80&auto=format&fit=crop",
                "Liver detoxification and metabolic recovery through structured nutritional care.",
                "Fatty liver disease occurs when fat builds up in the liver. Non-alcoholic fatty liver disease (NAFLD) is increasingly common due to poor diet.",
                List.of("Fatigue", "Abdominal discomfort", "Enlarged liver", "Often no symptoms in early stages"),
                List.of("Coffee — shown to reduce liver enzyme levels", "Green vegetables like broccoli and Brussels sprouts", "Whole grains and fiber-rich foods", "Walnuts and omega-3 rich foods"),
                List.of("Alcohol completely", "Sugary foods and fructose-rich beverages", "Refined carbohydrates and white bread", "Red meat and saturated fats"),
                "Losing just 7–10% of body weight can significantly reduce liver fat and inflammation.");

        // 11. Asthma
        seed("Asthma", "Wind", "#0891b2", "bg-cyan-50",
                "https://images.unsplash.com/photo-1584634731339-252c581abfc5?w=400&q=80&auto=format&fit=crop",
                "Nutritional support to reduce oxidative stress and support respiratory wellness.",
                "Asthma is a chronic respiratory condition causing airway inflammation and narrowing, leading to breathing difficulties and wheezing.",
                List.of("Shortness of breath", "Wheezing", "Chest tightness", "Chronic cough", "Difficulty sleeping"),
                List.of("Vitamin D rich foods to reduce airway inflammation", "Magnesium from leafy greens and nuts", "Antioxidants from fruits and vegetables", "Omega-3 fatty acids from fish"),
                List.of("Sulfite-containing foods like wine and dried fruits", "Food allergens if identified", "Processed foods with additives", "Obesity-promoting foods as excess weight worsens asthma"),
                "A Mediterranean-style diet has been associated with better asthma control and fewer attacks.");

        // 12. Kidney Disease
        seed("Kidney Disease", "Activity", "#7c3aed", "bg-purple-50",
                "https://images.unsplash.com/photo-1559757175-0eb30cd8c063?w=400&q=80&auto=format&fit=crop",
                "Renal-friendly diets with controlled mineral and protein levels for kidney health.",
                "Chronic kidney disease involves gradual loss of kidney function. Diet plays a critical role in slowing progression and managing symptoms.",
                List.of("Fatigue", "Swollen ankles and feet", "Shortness of breath", "Nausea", "Decreased urine output"),
                List.of("Low potassium foods like apples, grapes, and white rice", "Controlled protein intake", "Low phosphorus foods", "Adequate caloric intake to prevent muscle loss"),
                List.of("High potassium foods like bananas and potatoes", "High phosphorus foods like dairy and nuts", "Excess protein", "High sodium foods"),
                "Always work with a renal dietician as nutrient needs vary by stage of kidney disease.");

        // 13. GERD
        seed("GERD", "FlameKindling", "#ea580c", "bg-orange-50",
                "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400&q=80&auto=format&fit=crop",
                "Alkaline-based nutrition strategies to prevent acid reflux and soothe digestion.",
                "GERD is chronic acid reflux where stomach acid frequently flows back into the esophagus, causing irritation and discomfort.",
                List.of("Heartburn", "Regurgitation", "Difficulty swallowing", "Chest pain", "Chronic cough"),
                List.of("Alkaline foods — oatmeal, ginger, and green vegetables", "Lean proteins like chicken and fish", "Low-acid fruits like bananas and melons", "Small frequent meals throughout the day"),
                List.of("Spicy foods and citrus fruits", "Tomatoes and tomato-based products", "Chocolate, mint, and garlic", "Alcohol and caffeine", "Fatty and fried foods"),
                "Eating dinner at least 3 hours before bedtime significantly reduces nighttime acid reflux.");

        // 14. Depression
        seed("Depression", "Brain", "#6366f1", "bg-indigo-50",
                "https://images.unsplash.com/photo-1493836512294-502baa1986e2?w=400&q=80&auto=format&fit=crop",
                "Gut-brain axis nutrition focused on mood-regulating neurotransmitters.",
                "Depression is a mental health disorder causing persistent sadness. Emerging research shows strong links between gut health and mood.",
                List.of("Persistent sadness", "Loss of interest", "Sleep disturbances", "Fatigue", "Difficulty concentrating"),
                List.of("Omega-3 fatty acids from oily fish for brain health", "Fermented foods for gut microbiome support", "Tryptophan-rich foods like turkey and eggs", "B vitamins from whole grains and leafy greens"),
                List.of("Ultra-processed and fast foods", "Refined sugar causing mood crashes", "Alcohol which is a depressant", "Artificial sweeteners"),
                "Up to 90% of serotonin is produced in the gut — a healthy diet directly supports mental well-being.");

        // 15. Anxiety
        seed("Anxiety", "Sparkles", "#8b5cf6", "bg-violet-50",
                "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=400&q=80&auto=format&fit=crop",
                "Calming nutrients and magnesium-rich foods to support the nervous system.",
                "Anxiety disorders involve persistent excessive worry and fear. Nutrition plays a supporting role in nervous system regulation.",
                List.of("Excessive worry", "Restlessness", "Fatigue", "Difficulty concentrating", "Muscle tension", "Sleep problems"),
                List.of("Magnesium from dark chocolate, spinach, and almonds", "L-theanine from green tea for calm focus", "Complex carbohydrates for steady serotonin", "Probiotic foods for gut-brain health"),
                List.of("Caffeine and energy drinks", "Alcohol which disrupts sleep and mood", "High sugar foods causing blood sugar spikes", "Processed foods with artificial additives"),
                "Magnesium deficiency is strongly linked to anxiety — many people are deficient without knowing it.");

        // 16. Migraine
        seed("Migraine", "Pill", "#db2777", "bg-pink-50",
                "https://images.unsplash.com/photo-1552667466-07770ae110d0?w=400&q=80&auto=format&fit=crop",
                "Identifying dietary triggers and implementing preventative nutrition for relief.",
                "Migraines are intense recurring headaches often accompanied by nausea, vomiting, and sensitivity to light and sound.",
                List.of("Severe throbbing headache", "Nausea and vomiting", "Light and sound sensitivity", "Visual disturbances", "Dizziness"),
                List.of("Magnesium-rich foods like seeds and leafy greens", "Riboflavin (B2) from dairy and lean meats", "CoQ10 from fatty fish and organ meats", "Consistent meal timing"),
                List.of("Tyramine-rich foods like aged cheese and cured meats", "Alcohol especially red wine", "Caffeine or sudden withdrawal", "MSG and artificial sweeteners"),
                "Keeping a food diary to identify personal triggers is the most effective nutritional strategy for migraines.");

        // 17. Osteoporosis
        seed("Osteoporosis", "Shield", "#0284c7", "bg-sky-50",
                "https://images.unsplash.com/photo-1550572017-edd951b55104?w=400&q=80&auto=format&fit=crop",
                "Strengthen bone density with Calcium, Vitamin D, and K2 enriched diet plans.",
                "Osteoporosis causes bones to become weak and brittle. It often develops silently over years and is most common in postmenopausal women.",
                List.of("Usually no symptoms until fracture", "Back pain from fractured vertebrae", "Stooped posture", "Bone fractures from minor falls", "Loss of height over time"),
                List.of("Calcium from dairy, fortified plant milks, and leafy greens", "Vitamin D from sunlight and fatty fish", "Vitamin K2 from fermented foods and egg yolks", "Magnesium from nuts and seeds"),
                List.of("Excess alcohol which inhibits calcium absorption", "High sodium foods that cause calcium loss", "Soft drinks with phosphoric acid", "Excess caffeine"),
                "Weight-bearing exercise combined with calcium and Vitamin D is the most effective prevention strategy.");

        // 18. Cancer
        seed("Cancer", "Syringe", "#dc2626", "bg-red-50",
                "https://images.unsplash.com/photo-1576091160550-2173dba999ef?w=400&q=80&auto=format&fit=crop",
                "Precision nutritional therapy to support patients during and after medical treatment.",
                "Cancer involves abnormal cell growth. Nutrition plays a vital role in prevention, managing treatment side effects, and recovery.",
                List.of("Unexplained weight loss", "Fatigue", "Pain", "Skin changes", "Changes in bowel or bladder habits"),
                List.of("Cruciferous vegetables like broccoli and kale", "Antioxidant-rich berries and colorful produce", "Lean proteins to maintain muscle mass during treatment", "Anti-inflammatory omega-3 rich foods"),
                List.of("Processed and red meats", "Alcohol", "Ultra-processed foods with additives", "Excess sugar which may fuel cancer cell growth"),
                "A plant-forward diet rich in fiber can reduce the risk of colorectal cancer by up to 20%.");

        // 19. Stroke
        seed("Stroke", "Waves", "#0891b2", "bg-cyan-50",
                "https://images.unsplash.com/photo-1559757148-5c350d0d3c56?w=400&q=80&auto=format&fit=crop",
                "Vascular health optimization and neuro-protective dietary protocols.",
                "A stroke occurs when blood supply to part of the brain is cut off. Diet plays a key role in prevention by managing blood pressure and cholesterol.",
                List.of("Sudden numbness or weakness", "Confusion or trouble speaking", "Vision problems", "Severe headache", "Loss of balance"),
                List.of("DASH or Mediterranean diet principles", "Potassium and magnesium for blood pressure control", "Antioxidants to protect blood vessels", "Omega-3 fatty acids to reduce clot risk"),
                List.of("High sodium foods", "Trans and saturated fats", "Alcohol in excess", "Red and processed meats"),
                "Controlling blood pressure through diet is the single most effective way to prevent stroke.");

        // 20. Insomnia
        seed("Insomnia", "Moon", "#6366f1", "bg-indigo-50",
                "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?w=400&q=80&auto=format&fit=crop",
                "Foods and nutrients that promote healthy circadian rhythms and restful sleep.",
                "Insomnia is difficulty falling or staying asleep. Chronic insomnia affects overall health, mood, and cognitive function.",
                List.of("Difficulty falling asleep", "Waking frequently during the night", "Waking too early", "Daytime fatigue", "Irritability and difficulty concentrating"),
                List.of("Tryptophan-rich foods like turkey, milk, and bananas", "Melatonin-containing foods like tart cherries", "Magnesium from almonds and leafy greens", "Complex carbohydrates for serotonin production"),
                List.of("Caffeine after 2pm", "Alcohol which disrupts sleep cycles", "Heavy meals within 3 hours of bedtime", "High sugar foods causing blood sugar fluctuations"),
                "A small serving of tart cherry juice before bed has been shown to improve sleep duration naturally.");
    }

    private void seed(
            String name,
            String icon,
            String color,
            String bg,
            String image,
            String description,
            String overview,
            List<String> symptoms,
            List<String> nutrition,
            List<String> avoid,
            String tip
    ) {
        Disease d = Disease.builder()
                .name(name)
                .icon(icon)
                .color(color)
                .bg(bg)
                .image(image)
                .description(description)
                .overview(overview)
                .symptoms(diseaseService.serializeList(symptoms))
                .recommendedFoods(diseaseService.serializeList(nutrition))
                .foodsToAvoid(diseaseService.serializeList(avoid))
                .nutritionTips(tip)
                .status("ACTIVE")
                .build();
        diseaseRepository.save(d);
    }
}
