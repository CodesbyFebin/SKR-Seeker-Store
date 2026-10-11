#include <jni.h>
#include "llama.h"
#include <string>
#include <vector>
#include <memory>
#include <mutex>

static std::mutex inference_mutex;
struct Utf {
 JNIEnv* env; jstring value; const char* chars;
 Utf(JNIEnv* e,jstring v):env(e),value(v),chars(e->GetStringUTFChars(v,nullptr)){}
 ~Utf(){ if(chars) env->ReleaseStringUTFChars(value,chars); }
};
static void fail(JNIEnv* env,const char* msg){ env->ThrowNew(env->FindClass("java/lang/IllegalStateException"),msg); }
extern "C" JNIEXPORT jstring JNICALL
Java_com_codesbyfebin_skrdstore_LocalAgent_generate(JNIEnv* env,jobject,jstring path,jstring system,jstring question){
 std::lock_guard<std::mutex> lock(inference_mutex);
 try {
  Utf p(env,path),s(env,system),q(env,question);
  if(!p.chars || !s.chars || !q.chars) return nullptr;
  static std::once_flag initialized;
  std::call_once(initialized, [] { llama_backend_init(); });
  auto mp=llama_model_default_params(); mp.n_gpu_layers=0;
  std::unique_ptr<llama_model,decltype(&llama_model_free)> model(llama_model_load_from_file(p.chars,mp),llama_model_free);
  if(!model){fail(env,"Unable to load GGUF model");return nullptr;}
  auto cp=llama_context_default_params(); cp.n_ctx=2048; cp.n_batch=512; cp.n_threads=1; cp.n_threads_batch=1;
  std::unique_ptr<llama_context,decltype(&llama_free)> ctx(llama_init_from_model(model.get(),cp),llama_free);
  if(!ctx){fail(env,"Unable to allocate inference context");return nullptr;}
  llama_chat_message messages[]={{"system",s.chars},{"user",q.chars}};
  const char* tmpl=llama_model_chat_template(model.get(),nullptr);
  std::vector<char> formatted(16384);
  int len=llama_chat_apply_template(tmpl,messages,2,true,formatted.data(),formatted.size());
  if(len<0 || len>=(int)formatted.size()){fail(env,"Prompt template failed");return nullptr;}
  const auto* vocab=llama_model_get_vocab(model.get());
  int count=-llama_tokenize(vocab,formatted.data(),len,nullptr,0,true,true);
  if(count<1 || count>1792){fail(env,"Prompt exceeds model context");return nullptr;}
  std::vector<llama_token> tokens(count);
  if(llama_tokenize(vocab,formatted.data(),len,tokens.data(),count,true,true)<0){fail(env,"Tokenization failed");return nullptr;}
  for(int i=0;i<count;i+=512){ auto batch=llama_batch_get_one(tokens.data()+i,std::min(512,count-i)); if(llama_decode(ctx.get(),batch)!=0){fail(env,"Prompt decode failed");return nullptr;} }
  const char* grammar=R"GBNF(root ::= "1. " app ": " reason "\n2. " app ": " reason "\n3. " app ": " reason
app ::= "EdgeORE" | "Grass" | "Helium Mobile" | "Hivemapper" | "Jupiter" | "Phantom" | "Backpack" | "Kamino" | "Drift" | "LootGo"
reason ::= [A-Za-z0-9 ,.'()/+-]{8,96}
)GBNF";
  std::unique_ptr<llama_sampler,decltype(&llama_sampler_free)> sampler(llama_sampler_chain_init(llama_sampler_chain_default_params()),llama_sampler_free);
  auto constrained=llama_sampler_init_grammar(vocab,grammar,"root");
  if(!constrained){fail(env,"Recommendation grammar failed");return nullptr;}
  llama_sampler_chain_add(sampler.get(),constrained);
  llama_sampler_chain_add(sampler.get(),llama_sampler_init_greedy());
  std::string answer;
  for(int i=0;i<256;i++){
   auto token=llama_sampler_sample(sampler.get(),ctx.get(),-1);
   if(llama_vocab_is_eog(vocab,token)) break;
   char piece[512]; int n=llama_token_to_piece(vocab,token,piece,sizeof(piece),0,true);
   if(n<0){fail(env,"Token piece too large");return nullptr;}
   answer.append(piece,n);
   auto batch=llama_batch_get_one(&token,1);
   if(llama_decode(ctx.get(),batch)!=0){fail(env,"Generation decode failed");return nullptr;}
  }
  // Java UTF-8 decoder handles supplementary characters; NewStringUTF does not.
  jbyteArray bytes=env->NewByteArray(answer.size());
  env->SetByteArrayRegion(bytes,0,answer.size(),reinterpret_cast<const jbyte*>(answer.data()));
  jclass str=env->FindClass("java/lang/String");
  jmethodID ctor=env->GetMethodID(str,"<init>","([BLjava/lang/String;)V");
  jstring encoding=env->NewStringUTF("UTF-8");
  return static_cast<jstring>(env->NewObject(str,ctor,bytes,encoding));
 } catch(const std::exception& e){fail(env,e.what());return nullptr;}
}
