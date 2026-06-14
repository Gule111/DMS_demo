import os
import sys
from langchain_text_splitters import MarkdownHeaderTextSplitter
from langchain_chroma import Chroma

# 将根目录加到 sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..')))
from app.core.config import settings
from app.core.embeddings import SiliconFlowEmbeddings

CHROMA_DB_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'chroma_db'))

def build_vector_store():
    knowledge_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', 'data', 'knowledge'))
    print(f"[*] 开始读取知识库文档目录: {knowledge_dir}")

    if not os.path.exists(knowledge_dir):
        print(f"[Error] 知识库目录不存在: {knowledge_dir}")
        return

    all_chunks = []
    headers_to_split_on = [
        ("#", "Header 1"),
        ("##", "Header 2"),
    ]
    markdown_splitter = MarkdownHeaderTextSplitter(headers_to_split_on=headers_to_split_on)

    for file_name in os.listdir(knowledge_dir):
        if file_name.endswith('.md'):
            file_path = os.path.join(knowledge_dir, file_name)
            print(f"[*] 读取并切分文档: {file_name}")
            with open(file_path, 'r', encoding='utf-8') as f:
                text = f.read()
            
            chunks = markdown_splitter.split_text(text)
            # 在元数据中加上来源文件名
            for chunk in chunks:
                chunk.metadata["source"] = file_name
            all_chunks.extend(chunks)

    print(f"[Info] 切分完成，共生成 {len(all_chunks)} 个文本块。")

    # 初始化自定义 Embedding
    embeddings = SiliconFlowEmbeddings(
        model_name=settings.EMBEDDING_MODEL_NAME,
        api_key=settings.EMBEDDING_API_KEY,
        base_url=settings.EMBEDDING_BASE_URL
    )

    # 存储到 ChromaDB
    print(f"[*] 正在写入并创建 Chroma 向量索引至: {CHROMA_DB_PATH} ...")
    db = Chroma.from_documents(
        documents=all_chunks,
        embedding=embeddings,
        persist_directory=CHROMA_DB_PATH
    )
    print("[Success] ChromaDB 向量数据库创建并持久化成功！")
    return db

if __name__ == '__main__':
    build_vector_store()
